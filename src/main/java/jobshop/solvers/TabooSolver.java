package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabooSolver implements Solver {

    static class Block {

        final int machine;
        final int firstTask;
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }

        public String toString() {
            return "Block: M" + machine + ", i= " + firstTask + " - " + lastTask;
        }
    }

    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        public void applyOn(ResourceOrder order) {
            Task tampon = order.tasksByMachine[this.machine][t1];
            order.tasksByMachine[this.machine][t1] = order.tasksByMachine[this.machine][t2];
            order.tasksByMachine[this.machine][t2] = tampon;
        }

        @Override
        public String toString() {
            return "Swap{" +
                    "machine=" + machine +
                    ", t1=" + t1 +
                    ", t2=" + t2 +
                    '}';
        }
    }


    @Override
    public Result solve(Instance instance, long deadline) {
        // Parameters
        int dureeTaboo = 5;
        int maxIter = 200;


        GreedySolver greedy = new GreedySolver();
        ResourceOrder sol_initiale = new ResourceOrder(greedy.solve(instance, deadline).schedule);

        ResourceOrder meilleure_sol = sol_initiale.copy();
        ResourceOrder sol = sol_initiale.copy();

        int[][] solTaboo = new int[instance.numMachines*instance.numJobs][instance.numMachines*instance.numJobs];
        for(int[] array : solTaboo) {
            Arrays.fill(array, 0);
        }

        int n_iter = 0;
        boolean timeout = false;
        boolean isEnhanced = true;

        while (!timeout && n_iter < maxIter) {
            n_iter++;
            isEnhanced = false;

            ArrayList<Swap> voisinage = new ArrayList<>();
            for (Block block : this.blocksOfCriticalPath(sol)) {
                voisinage.addAll(this.neighbors(block));
            }

                    int min = Integer.MAX_VALUE;
            Swap best_neighbor = null;

              for (Swap voisin : voisinage) {

                int i = voisin.t1;
                i += voisin.machine * instance.numJobs;
                int j = voisin.t2;
                j += voisin.machine * instance.numJobs;
                boolean taboo = n_iter < solTaboo[i][j];

                ResourceOrder tmp;
                if (!taboo) {
                    tmp = sol.copy();
                    voisin.applyOn(tmp);
                    int makespan = tmp.toSchedule().makespan();
                    if (makespan < min) {
                        min = makespan;
                        best_neighbor = voisin;
                    }
                } else {

                }
            }

            if (best_neighbor != null) {
                int i = best_neighbor.t1;
                i += best_neighbor.machine * instance.numJobs;
                int j = best_neighbor.t2;
                j += best_neighbor.machine * instance.numJobs;
                solTaboo[i][j] = n_iter + dureeTaboo;


                best_neighbor.applyOn(sol);


                if (sol.toSchedule().makespan() < meilleure_sol.toSchedule().makespan()) {
                    isEnhanced = true;
                    meilleure_sol = sol.copy();
                }
            }

            timeout = (deadline - System.currentTimeMillis()) <= 1;

        }

        return new Result(instance, meilleure_sol.toSchedule(), timeout ? Result.ExitCause.Timeout : Result.ExitCause.Blocked);
    }


    public List<Block> blocksOfCriticalPath(ResourceOrder order) {
        ArrayList<Block> result = new ArrayList<>();

        List<Task> criticalPath = order.toSchedule().criticalPath();

        int i = 0;
        while (i < criticalPath.size()) {
            int machine = order.instance.machine(criticalPath.get(i).job, criticalPath.get(i).task);

            boolean trouve = false;
            int j = i + 1;
            while (j < criticalPath.size() && order.instance.machine(criticalPath.get(j).job, criticalPath.get(j).task) == machine) {
                trouve = true;
                j++;
            }

            if (trouve) {
                Task firstTask = criticalPath.get(i);
                Task lastTask = criticalPath.get(j - 1);

               int index = 0;
                while (index < order.tasksByMachine[machine].length && !order.tasksByMachine[machine][index].equals(firstTask)) {
                    index++;
                }
                int first = index;

                index = 0;
                while (index < order.tasksByMachine[machine].length && !order.tasksByMachine[machine][index].equals(lastTask)) {
                    index++;
                }
                int last = index;

                result.add(new Block(machine, first, last));
                i = j;
            } else {
                i++;
            }
        }

        return result;

    }

      public List<Swap> neighbors(Block block) {
        ArrayList<Swap> result = new ArrayList<>();

        if (block.lastTask - block.firstTask >= 2) { // 3 elem or more
            result.add(new Swap(block.machine, block.firstTask, block.firstTask + 1));
            result.add(new Swap(block.machine, block.lastTask, block.lastTask - 1));
        } else {
            result.add(new Swap(block.machine, block.firstTask, block.lastTask));
        }

        return result;
    }
}