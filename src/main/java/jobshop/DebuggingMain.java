package jobshop;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.solvers.DescentSolver;

import java.io.IOException;
import java.nio.file.Paths;

public class DebuggingMain {

    public static void main(String[] args) {
        try {

            Instance instance = Instance.fromFile(Paths.get("instances/aaa1"));


            JobNumbers enc = new JobNumbers(instance);
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;

            System.out.println("\nENCODING: " + enc);

            Schedule sched = enc.toSchedule();
            System.out.println("SCHEDULE: " + sched);
            System.out.println("VALID: " + sched.isValid());
            System.out.println("MAKESPAN: " + sched.makespan());


            System.out.println("ResourceOrder fromSchedule tests:");
            ResourceOrder ro = new ResourceOrder(instance);

            System.out.println("JobNumbers fromSchedule tests:");
            JobNumbers jn = new JobNumbers(instance);


        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
