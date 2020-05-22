package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class DescentSolver implements Solver {
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
         GreedySolver greedy = new GreedySolver();
        ResourceOrder sol_initiale = new ResourceOrder(greedy.solve(instance, deadline).schedule);

        ResourceOrder best_solution = sol_initiale.copy();

        boolean timeout = false;
        boolean isEnhanced = true;

        while (isEnhanced && !timeout) {

            isEnhanced = false;
            ArrayList<Swap> voisinage = new ArrayList<>();
            for (Block block : this.blocksOfCriticalPath(best_solution)) {
                voisinage.addAll(this.neighbors(block));
            }

            if (voisinage.size() > 0) {

                Swap best = voisinage.get(0);
                int min = best_solution.toSchedule().makespan(); // will it enhance the current best sol

                for (Swap s : voisinage) {
                    ResourceOrder temp = best_solution.copy();
                    s.applyOn(temp);

                    int val = temp.toSchedule().makespan();
                    if (val < min) {
                        best = s;
                        min = val;
                        isEnhanced = true;
                    }
                }

                if (isEnhanced){
                    best.applyOn(best_solution);
                }

            }

            timeout = (deadline - System.currentTimeMillis()) <= 1;

        }

        return new Result(instance, best_solution.toSchedule(), timeout ? Result.ExitCause.Timeout : Result.ExitCause.Blocked);
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
                Task lastTask = criticalPath.get(j-1);

                int index = 0;
                while (index < order.tasksByMachine[machine].length && !order.tasksByMachine[machine][index].equals(firstTask)){
                    index++;
                }
                int first = index;

                index = 0;
                while (index < order.tasksByMachine[machine].length && !order.tasksByMachine[machine][index].equals(lastTask)){
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
