package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class JobNumbers extends Encoding {


    public final int[] jobs;

    public int nextToSet = 0;

    public JobNumbers(Instance instance) {
        super(instance);

        jobs = new int[instance.numJobs * instance.numMachines];
        Arrays.fill(jobs, -1);
    }

    public JobNumbers(Schedule schedule) {
        super(schedule.pb);

        this.jobs = new int[instance.numJobs * instance.numTasks];

        // for each job indicates which is the next task to be scheduled
        int[] nextOnJob = new int[instance.numJobs];

        while(Arrays.stream(nextOnJob).anyMatch(t -> t < instance.numTasks)) {
            Task next = IntStream
                    .range(0, instance.numJobs)
                    .mapToObj(j -> new Task(j, nextOnJob[j]))
                    .filter(t -> t.task < instance.numTasks)
                    .min(Comparator.comparing(t -> schedule.startTime(t.job, t.task)))
                    .get();

            this.jobs[nextToSet++] = next.job;
            nextOnJob[next.job] += 1;
        }
    }

    @Override
    public Schedule toSchedule() {
        int[] nextFreeTimeResource = new int[instance.numMachines];
        int[] nextTask = new int[instance.numJobs];
        int[][] startTimes = new int[instance.numJobs][instance.numTasks];
        for(int job : jobs) {
            int task = nextTask[job];
            int machine = instance.machine(job, task);
            int est = task == 0 ? 0 : startTimes[job][task-1] + instance.duration(job, task-1);
            est = Math.max(est, nextFreeTimeResource[machine]);

            startTimes[job][task] = est;
            nextFreeTimeResource[machine] = est + instance.duration(job, task);
            nextTask[job] = task + 1;
        }

        return new Schedule(instance, startTimes);
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOfRange(jobs,0, nextToSet));
    }
}
