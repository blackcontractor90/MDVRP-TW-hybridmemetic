# MDVRPTWSolver — Enhanced Memetic Solver for MDVRPTW (MDVRP-TW-Memetic)

A JavaFX GUI front-end to solve the Multi-Depot Vehicle Routing Problem with Time Windows (MDVRPTW) using an enhanced memetic algorithm combined with hybrid local search (route relocation) and LNS reinsertion strategies. This README is tailored specifically to the MDVRPTWSolver.java source you provided and documents how the file fits in the project, expected dependencies, how to run and test, and important implementation notes and TODOs.

Table of contents
- Overview
- Requirements
- Expected project layout & required classes
- Data / dataset format supported
- Build & run (quick)
- GUI features & user workflow
- Algorithm integration points
- Outputs & saved artifacts
- Known issues, inconsistencies & TODOs (actionable)
- Recommended next steps
- License

Overview
--------
MDVRPTWSolver.java is a JavaFX Application that:
- Provides an interactive canvas (CanvasPane) to display depots, customers, and solution routes.
- Loads MDVRPTW datasets (DataLoader or fallback Cordeau-style parser).
- Exposes algorithm parameters (population, crossover, scaling, generations, capacity, penalties).
- Runs a MemeticAlgorithm (ga + local search) on a background thread and updates a convergence chart (LineChart).
- Applies route relocation local search and LNS (Large Neighborhood Search) reinsertion on solutions.
- Exports run metrics and saves canvas snapshots.

Requirements
------------
- Java 11+ (OpenJDK recommended).
- JavaFX (OpenJFX) for your Java version (modules: javafx.controls, javafx.graphics; javafx.swing if you use Swing-based image utilities).
- Build tool (recommended): Maven or Gradle to manage JavaFX dependencies and runnable packaging.

Expected project layout & required classes
------------------------------------------
MDVRPTWSolver depends on several domain and utility classes. Implement these or adapt the solver to your existing project.

Core domain classes (interfaces / fields / methods used by MDVRPTWSolver):
- Customer
  - Fields referenced: double x, double y, int demand, double readyTime, double dueTime, double serviceTime, int assignedDepotId
  - Used methods expected: double distanceTo(Customer), double distanceTo(Depot) (MDVRPTWSolver also uses direct coordinate access).
  - Constructor usage: new Customer(x, y, "C" + id, demand, readyTime, dueTime, serviceTime)

- Depot
  - Fields referenced: double x, double y, int maxVehicles, int vehicleCapacity
  - Methods expected: double distanceTo(Customer)
  - Constructor usage: new Depot(x, y, "D" + index)

- Route
  - Fields referenced: Depot depot; List<Customer> customers; double distance; double penalty; int timeWindowViolations; javafx.scene.paint.Color color
  - Methods expected:
    - double computeTotalDistance() — used by LNS
    - void evaluateTimeWindows() — optionally used by evaluateFitness in this file
  - Constructor usage: new Route(depot)

- Solution
  - Fields/methods expected: List<Route> routes; double fitness, totalDistance, totalPenalty; int[] or List<Integer> chromosome; copy constructor new Solution(Solution)
  - Constructor usage: new Solution(int[] chromosome) and new Solution(Solution other)

Supporting utilities:
- CanvasPane
  - Responsibilities: draw depots/customers/routes, save snapshot, toggles for labels/time-windows.
  - Methods used: setDepots(...), setCustomers(...), setSolutionRoutes(...), setShowDepotLabels(...), setShowCustomerLabels(...), setShowRouteLabels(...), setShowTimeWindows(...), draw(), drawWithGenerationOverlay(...), saveSnapshot(String), setAddDepotMode(), setAddCustomerMode()

- RoutingSolver
  - A thin UI wrapper: setCanvasPane(CanvasPane), setDepots(List<Depot>), setCustomers(List<Customer>), solve(), getRoutes(), decodeSolution(Solution) (note: evaluateFitness uses solver.decodeSolution)

- MemeticAlgorithm
  - Expected constructor: MemeticAlgorithm(MDVRPTWSolver ui, int populationSize, double scalingFactor, double crossoverRate, int maxGenerations)
  - Methods: void setConvergenceSeries(XYChart.Series<Number, Number> series), Solution run()

- DataLoader (optional)
  - DataLoader.loadData(path) populates loader.depots, loader.customers and loader.vehicleCapacity

- RouteExporter, SolutionMetrics, MetricsAnalyzer, MetricsChartViewer
  - Utilities used for CSV export, saving run metrics, analyzing summaries and showing charts.

Data / dataset format supported
------------------------------
- MDVRPTWSolver attempts to load data with DataLoader first, and falls back to a Cordeau-compatible parser implemented in parseCordeauFile(File).
- The Cordeau parser expects a header:
  [totalVehicles] [depotCount] [customerCount] [vehicleCapacity]
  followed by depot time-window lines (skipped), then customer lines (id x y serviceTime demand [ready due]), then depot coordinates (x y).
- If you use a different dataset format, either implement DataLoader.loadData or adapt parseCordeauFile.

Build & run (quick)
-------------------
This is a JavaFX application—JavaFX must be supplied at runtime.

Example with Maven:
- Add OpenJFX dependencies and (optionally) javafx-maven-plugin to your pom.xml.
- Build: mvn clean package
- Run example:
  java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -cp target/your-jar.jar MDVRPTWSolver

Using javac/java directly:
- Compile:
  javac --module-path /path/to/javafx/lib --add-modules javafx.controls -cp . MDVRPTWSolver.java
- Run:
  java --module-path /path/to/javafx/lib --add-modules javafx.controls -cp . MDVRPTWSolver

GUI features & user workflow
----------------------------
- Top menu: File (New, Load dataset, Export CSV, Save Image), Edit (Add Depot/Customer, Clear), View toggles, Help → About.
- Right control panel: Algorithm parameter spinners, toggles for local search, Run button, Analyze Metrics Summary button, visualization toggles.
- Bottom: Convergence line chart (generations vs best fitness) and a log area.
- Canvas: interactive depot/customer addition, route visualization and snapshot saving.

Algorithm integration points
----------------------------
- The UI starts a MemeticAlgorithm instance on a background thread and uses its run() result (Solution).
- The MemeticAlgorithm should update the provided convergenceSeries by adding XYChart.Data points from the JavaFX thread (Platform.runLater).
- After a best solution is returned, MDVRPTWSolver applies optional route relocation local search and LNS, saves run metrics, updates UI, and saves a final snapshot.

Outputs & saved artifacts
------------------------
- Canvas snapshots via canvasPane.saveSnapshot("final_<runId>").
- CSV exports via RouteExporter (user-chosen file).
- Run metrics via SolutionMetrics.saveRunToCSV (saved to a results folder — implement this utility to match your structure).
- Convergence chart is visible in-app; you may persist it using JavaFX snapshot utilities (not in this file).

Known issues, inconsistencies & TODOs (actionable)
-------------------------------------------------

1) createRandomSolution() — logic bug
- Current implementation builds an int[] chromosome, then does:
  Collections.shuffle(Arrays.asList(Arrays.stream(chromosome).boxed().toArray(Integer[]::new)));
  but never writes the shuffled order back into the primitive int[].
- Result: the chromosome remains in ascending order and the returned Solution is not randomized.
- Fix: convert to List<Integer>, shuffle that list, then copy back to int[] (or use an IntStream to generate a randomized permutation).
  Example (concept):
  List<Integer> list = IntStream.range(0, customers.size()).boxed().collect(Collectors.toList());
  Collections.shuffle(list, rand);
  int[] chromosome = list.stream().mapToInt(Integer::intValue).toArray();

2) Inconsistent route distance/field names
- The code uses route.distance in evaluateRoute, but evaluateFitness accumulates route.totalDistance.
- Ensure Route defines a consistent field name (either distance or totalDistance) and methods computeTotalDistance() and evaluateTimeWindows() update those fields consistently.
- Standardize on route.distance (or route.totalDistance) everywhere and update callers.

3) evaluateFitness uses solver.decodeSolution and route.evaluateTimeWindows
- evaluateFitness calls solver.decodeSolution(solution). But in this file MDVRPTWSolver already provides decodeSolution(Solution). Decide which one owns decoding; avoid duplication.
- route.evaluateTimeWindows() is invoked but not guaranteed to exist on Route. Either implement evaluateTimeWindows() on Route or use evaluateRoute(route) from MDVRPTWSolver to compute distance and penalty.

4) Duplicate evaluation methods / naming collisions
- This file defines both evaluateSolution(Solution) and evaluate(Solution). They are very similar; pick a single canonical method to avoid confusion and duplication.
- Evaluate and evaluateSolution differ slightly in how routes are decoded or measured — unify them.

5) Distance helpers: multiple variants exist
- Methods: distance(Depot, Customer), distance(Customer, Customer), and distance(x1,y1,x2,y2) exist. Remove duplicates or centralize in a utility (e.g., Geometry.distance(a,b)) or delegate to Depot/Customer distanceTo(...) methods.

6) Null-safety and empty lists
- findNearestDepot assumes depots.size() > 0. Guard calls where depots can be empty to avoid exceptions.

7) Concurrency and JavaFX thread-safety
- MemeticAlgorithm.run() must not modify JavaFX UI directly; use Platform.runLater for chart updates.
- When adding data to convergenceSeries from background threads, ensure Platform.runLater is used.

8) LNS reinsertion uses route.computeTotalDistance()
- Ensure Route.computeTotalDistance() returns the same metric evaluateRoute produces; if not, results may be inconsistent.

9) Missing getPenaltyWeight() used in evaluateFitness
- evaluateFitness reads penaltyWeight via solver.getPenaltyWeight(); ensure RoutingSolver exposes this accessor or change to read MDVRPTWSolver.penaltyWeight directly.

10) API surface stability
- Decide which classes (RoutingSolver vs MDVRPTWSolver) provide decoding, evaluation, and solution constructors — centralize responsibilities to avoid confusion.

Recommended immediate code fixes (haven't done it yet!)
- Fix createRandomSolution permutation bug.
- Unify route distance/penalty field names.
- Remove duplicated evaluate() / evaluateSolution(); keep one and update callers.
- Add guards in findNearestDepot and other depot-dependent methods.

Maintainer: blackcontractor90 (https://www.linkedin.com/in/farid-morsidi-372083141/)
