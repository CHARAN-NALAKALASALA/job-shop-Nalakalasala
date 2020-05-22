package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

public class ResourceOrder extends Encoding {

    public final Task[][] tasksByMachine;

     public final int[] nextFreeSlot;

     public ResourceOrder(Instance instance)
    {
        super(instance);

        tasksByMachine = new Task[instance.numMachines][instance.numJobs];

        nextFreeSlot = new int[instance.numMachines];
    }

    public ResourceOrder(Schedule schedule)
    {
        super(schedule.pb);
        Instance pb = schedule.pb;

        this.tasksByMachine = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        for(int m = 0 ; m<schedule.pb.numMachines ; m++) {
            final int machine = m;


            tasksByMachine[m] =
                    IntStream.range(0, pb.numJobs) // all job numbers
                            .mapToObj(j -> new Task(j, pb.task_with_machine(j, machine))) // all tasks on this machine (one per job)
                            .sorted(Comparator.comparing(t -> schedule.startTime(t.job, t.task))) // sorted by start time
                            .toArray(Task[]::new); // as new array and store in tasksByMachine

                       nextFreeSlot[m] = instance.numJobs;
        }
    }

    @Override
    public Schedule toSchedule() {
        int [][] startTimes = new int [instance.numJobs][instance.numTasks];

        int[] nextToScheduleByJob = new int[instance.numJobs];

        int[] nextToScheduleByMachine = new int[instance.numMachines];

        int[] releaseTimeOfMachine = new int[instance.numMachines];

        while(IntStream.range(0, instance.numJobs).anyMatch(m -> nextToScheduleByJob[m] < instance.numTasks)) {

            Optional<Task> schedulable =
                    IntStream.range(0, instance.numMachines)
                    .filter(m -> nextToScheduleByMachine[m] < instance.numJobs)
                            .mapToObj(m -> this.tasksByMachine[m][nextToScheduleByMachine[m]])

                    .filter(task -> task.task == nextToScheduleByJob[task.job])
                    .findFirst();

            if(schedulable.isPresent()) {
                Task t = schedulable.get();
                int machine = instance.machine(t.job, t.task);
                int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
                est = Math.max(est, releaseTimeOfMachine[instance.machine(t)]);
                startTimes[t.job][t.task] = est;

                nextToScheduleByJob[t.job]++;
                nextToScheduleByMachine[machine]++;

                releaseTimeOfMachine[machine] = est + instance.duration(t.job, t.task);
            } else {
                return null;
            }
        }
             return new Schedule(instance, startTimes);
    }


    public ResourceOrder copy() {
        return new ResourceOrder(this.toSchedule());
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for(int m=0; m < instance.numMachines; m++)
        {
            s.append("Machine ").append(m).append(" : ");
            for(int j=0; j<instance.numJobs; j++)
            {
                s.append(tasksByMachine[m][j]).append(" ; ");
            }
            s.append("\n");
        }

        return s.toString();
    }

}