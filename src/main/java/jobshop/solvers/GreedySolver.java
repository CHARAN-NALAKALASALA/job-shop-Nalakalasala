package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GreedySolver implements Solver {

    public enum Rule {
        SPT, LRPT, EST_SPT, EST_LRPT;
    }

    @Override
    public Result solve(Instance instance, long deadline) {

        Rule rule = Rule.EST_LRPT;

        ResourceOrder sol = new ResourceOrder(instance);
        Arrays.fill(sol.nextFreeSlot, 0);

        ArrayList<Task> taches_realisables = new ArrayList<>();
        for (int job = 0; job < instance.numJobs; job++) {
            taches_realisables.add(new Task(job, 0));
        }

        int machineStartTimes[] = new int[instance.numMachines];
        int jobStartTimes[] = new int[instance.numJobs];
        Arrays.fill(machineStartTimes, 0);
        Arrays.fill(jobStartTimes, 0);

        boolean timeout = false;
        while (!taches_realisables.isEmpty() && !timeout) {

            ArrayList<Task> selected = new ArrayList<>();
            int bestStartTime = 0;

            if (rule == Rule.EST_LRPT || rule == Rule.EST_SPT) {
                int min = Integer.MAX_VALUE;
                for (Task tache : taches_realisables) {
                    int start_time = Integer.max(machineStartTimes[instance.machine(tache)], jobStartTimes[tache.job]);

                    if (start_time < min) {
                        min = start_time;
                        selected.clear();
                        selected.add(tache);
                    } else if (start_time == min) {
                        selected.add(tache);
                    }
                }
                bestStartTime = min;
            } else {
                selected = taches_realisables;
            }
           Task bestTask = this.select(selected, instance, rule);

            taches_realisables.remove(bestTask);
            int machine = instance.machine(bestTask);
            sol.tasksByMachine[machine][sol.nextFreeSlot[machine]] = bestTask;
            sol.nextFreeSlot[machine]++;

            machineStartTimes[machine] = bestStartTime + instance.duration(bestTask);
            jobStartTimes[bestTask.job] = bestStartTime + instance.duration(bestTask);

            if (bestTask.task < instance.numTasks - 1) {
                taches_realisables.add(new Task(bestTask.job, bestTask.task + 1));
            }

            timeout = (deadline - System.currentTimeMillis()) <= 1;
        }

        return new Result(instance, sol.toSchedule(), timeout ? Result.ExitCause.Timeout : Result.ExitCause.Blocked);
    }

    private Task select(ArrayList<Task> taches_realisables, Instance instance, Rule rule) {

        switch (rule) {
            case EST_SPT:
                rule = Rule.SPT;
                break;
            case EST_LRPT:
                rule = Rule.LRPT;
                break;
            default:
                break;
        }

        Task result = taches_realisables.get(0);
        int val = instance.duration(result.job, result.task);

        for (Task t : taches_realisables) {

            if (rule == Rule.SPT) {
                int duration = instance.duration(t.job, t.task);

                if (duration < val) {
                    result = t;
                    val = duration;
                }
            } else if (rule == Rule.LRPT) {
                int sum = 0;
                for (int remaining_task = t.task; remaining_task < instance.numTasks; remaining_task++) {
                    sum += instance.duration(t.job, remaining_task);
                }

                if (sum > val) {
                    result = t;
                    val = sum;
                }
            }

        }

        return result;
    }
}
