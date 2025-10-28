import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class MemeticAlgorithm {
    private final MDVRPTWSolver solver;
    private final int populationSize;
    private final double maxGenerations;
    private final double crossoverRate;
    private final double mutationRate;
    private final Random rand = new Random();

    private XYChart.Series<Number, Number> convergenceSeries;
    private final int maxLocalSearchIterations = 50;
    private final int eliteLocalSearchCount = 5;

    public MemeticAlgorithm(MDVRPTWSolver solver, int populationSize, double maxGenerations,
                            double crossoverRate, double mutationRate) {
        this.solver = solver;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
    }

    public void setConvergenceSeries(XYChart.Series<Number, Number> series) {
        this.convergenceSeries = series;
    }

    public Solution run() {
        List<Solution> population = initializePopulation();
        Solution bestSolution = findBestSolution(population);

        for (int gen = 0; gen < maxGenerations; gen++) {
            List<Solution> newPopulation = new ArrayList<>();
            newPopulation.add(new Solution(bestSolution)); // Elitism

            while (newPopulation.size() < populationSize) {
                Solution parent1 = tournamentSelection(population);
                Solution parent2 = tournamentSelection(population);

                Solution child1 = new Solution(parent1);
                Solution child2 = new Solution(parent2);

                if (rand.nextDouble() < crossoverRate) {
                    pmxCrossover(child1, child2);
                }

                mutate(child1);
                mutate(child2);

                solver.evaluate(child1);
                solver.evaluate(child2);

                newPopulation.add(child1);
                newPopulation.add(child2);
            }

            // Evaluate and apply local search to top-k individuals
            newPopulation.sort(Comparator.comparingDouble(sol -> sol.fitness));
            for (int i = 0; i < Math.min(eliteLocalSearchCount, newPopulation.size()); i++) {
                hybridLocalSearch(newPopulation.get(i));
            }

            population = newPopulation;
            Solution currentBest = findBestSolution(population);
            if (currentBest.fitness < bestSolution.fitness) {
                bestSolution = new Solution(currentBest);
            }

            updateConvergenceChart(gen, bestSolution.fitness);

            if (gen % 5 == 0 && solver != null) {
                final int g = gen;
                final Solution solutionToShow = new Solution(bestSolution);
                Platform.runLater(() -> solver.animateSolution(solutionToShow, g));
            }
        }

        return bestSolution;
    }

    private List<Solution> initializePopulation() {
        List<Solution> pop = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            Solution sol = solver.createRandomSolution();
            if (sol != null) {
                solver.evaluate(sol);
                pop.add(sol);
            }
        }
        return pop;
    }

    private Solution findBestSolution(List<Solution> population) {
        return population.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(sol -> sol.fitness))
                .orElseThrow(() -> new IllegalStateException("Population is empty or contains no valid solutions"));
    }

    private Solution tournamentSelection(List<Solution> population) {
        int k = 3;
        Solution best = population.get(rand.nextInt(population.size()));
        for (int i = 1; i < k; i++) {
            Solution challenger = population.get(rand.nextInt(population.size()));
            if (challenger.fitness < best.fitness) {
                best = challenger;
            }
        }
        return new Solution(best);
    }

    private void pmxCrossover(Solution a, Solution b) {
        int size = a.chromosome.length;
        int point1 = rand.nextInt(size);
        int point2 = rand.nextInt(size);
        if (point1 > point2) {
            int temp = point1;
            point1 = point2;
            point2 = temp;
        }

        int[] child1 = new int[size];
        int[] child2 = new int[size];
        Arrays.fill(child1, -1);
        Arrays.fill(child2, -1);

        System.arraycopy(a.chromosome, point1, child1, point1, point2 - point1 + 1);
        System.arraycopy(b.chromosome, point1, child2, point1, point2 - point1 + 1);

        for (int i = point1; i <= point2; i++) {
            int gene1 = a.chromosome[i];
            int gene2 = b.chromosome[i];
            for (int j = 0; j < size; j++) {
                if (child1[j] == -1 && b.chromosome[j] == gene1) child1[j] = gene2;
                if (child2[j] == -1 && a.chromosome[j] == gene2) child2[j] = gene1;
            }
        }

        for (int i = 0; i < size; i++) {
            if (child1[i] == -1) child1[i] = b.chromosome[i];
            if (child2[i] == -1) child2[i] = a.chromosome[i];
        }

        a.chromosome = child1;
        b.chromosome = child2;
    }

    private void mutate(Solution sol) {
        if (rand.nextDouble() > mutationRate || sol.chromosome.length < 2) return;
        int i = rand.nextInt(sol.chromosome.length);
        int j = rand.nextInt(sol.chromosome.length);
        int temp = sol.chromosome[i];
        sol.chromosome[i] = sol.chromosome[j];
        sol.chromosome[j] = temp;
    }

    private void hybridLocalSearch(Solution sol) {
        int iteration = 0;
        boolean improved;
        do {
            improved = false;
            iteration++;

            // Intra-route 2-opt
            for (Route route : sol.routes) {
                List<Customer> customers = route.customers;
                int size = customers.size();
                for (int i = 1; i < size - 1; i++) {
                    for (int j = i + 1; j < size; j++) {
                        Collections.reverse(customers.subList(i, j + 1));
                        double oldFitness = sol.fitness;
                        solver.evaluate(sol);
                        if (sol.fitness < oldFitness) {
                            improved = true;
                        } else {
                            Collections.reverse(customers.subList(i, j + 1));
                            sol.fitness = oldFitness;
                        }
                    }
                }
            }

            // Inter-route relocate
            outer:
            for (int i = 0; i < sol.routes.size(); i++) {
                Route from = sol.routes.get(i);
                for (int j = 0; j < sol.routes.size(); j++) {
                    if (i == j) continue;
                    Route to = sol.routes.get(j);
                    for (int c = 0; c < from.customers.size(); c++) {
                        Customer customer = from.customers.get(c);
                        double oldFitness = sol.fitness;
                        from.customers.remove(c);
                        to.customers.add(customer);
                        solver.evaluate(sol);
                        if (sol.fitness < oldFitness) {
                            improved = true;
                            break outer;
                        } else {
                            to.customers.remove(to.customers.size() - 1);
                            from.customers.add(c, customer);
                            sol.fitness = oldFitness;
                        }
                    }
                }
            }

        } while (improved && iteration < maxLocalSearchIterations);
    }

    private void updateConvergenceChart(int generation, double fitness) {
        if (convergenceSeries != null) {
            Platform.runLater(() -> convergenceSeries.getData().add(new XYChart.Data<>(generation, fitness)));
        }
    }
}
