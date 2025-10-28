# MDVRP-TW-hybridmemetic
Solving the Multi‑Depot Vehicle Routing Problem with Time Windows (MDVRPTW). This program wires an enhanced memetic (genetic) algorithm, optional relocation local search and LNS repair, supports Cordeau-style and CSV dataset loading, and provides interactive visualizations (route canvas, convergence chart), logging, and CSV/image export. 

Contains:
-JavaFX UI with parameter controls (population size, crossover rate, scaling factor, generations, vehicle capacity, penalty)
-Cordeau-compatible dataset parser + fallback DataLoader integration
-Enhanced Memetic Algorithm orchestration with convergence plotting and animation hooks
-Hybrid local search: route relocation and a basic Large Neighborhood Search (LNS) destroy‑and‑repair
-Route evaluation including distance, time-window handling, penalties and feasibility checks
-Export hooks: save solution snapshots (PNG) and export routes/metrics to CSV; visualize metrics
-Designed to integrate with external helper classes: CanvasPane, RoutingSolver, MemeticAlgorithm, RouteExporter, SolutionMetrics, MetricsAnalyzer

Technical notes / expected environment:
-Language: Java (uses JavaFX) — run with Java 11+ (Java 17+ recommended) and matching JavaFX modules.
-Entry point: public static void main(String[] args) — launches JavaFX Application.
-This file expects the following supporting domain/systems classes to be present on the classpath:
-Domain: Customer (x,y,demand,readyTime,dueTime,serviceTime,assignedDepotId), Depot (x,y,vehicleCapacity,maxVehicles,distanceTo), Route (customers list, depot, methods computeTotalDistance(), evaluateTimeWindows(), fields distance/penalty/timeWindowViolations), Solution (chromosome, routes, fitness, totalDistance/totalPenalty, copy ctor).
-Solver & UI helpers: RoutingSolver, MemeticAlgorithm, CanvasPane, RouteExporter, SolutionMetrics, MetricsAnalyzer.
-File I/O: uses FileChooser/DirectoryChooser for load/save; parseCordeauFile expects a specific header layout described in the code.
-Threading: algorithm runs on a background Thread with Platform.runLater() used to update UI.

Quick usage hint:
Build with your Java toolchain and ensure JavaFX is available at runtime (module path or dependencies).
Run: java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml MDVRPTWSolver
In the UI: File → Load Cordeau Dataset, tune algorithm parameters in the control panel, then click the Run button to start optimization and visualize results.
