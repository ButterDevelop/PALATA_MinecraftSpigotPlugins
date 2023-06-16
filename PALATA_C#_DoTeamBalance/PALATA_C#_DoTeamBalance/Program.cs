using PALATA_C__DoTeamBalance;

/*var graph = new Graph();
// добавить все ребра графа
graph.Edges.Add(new Edge(1, 2, 5));
graph.Edges.Add(new Edge(2, 1, 5));
graph.Edges.Add(new Edge(2, 4, 4));
graph.Edges.Add(new Edge(3, 5, 5));
graph.Edges.Add(new Edge(5, 3, 5));
graph.Edges.Add(new Edge(6, 5, 5));
graph.Edges.Add(new Edge(5, 6, 5));
// ...

var sa = new SimulatedAnnealing(10, 0.000001);
var initialSolution = new Solution(graph);
initialSolution.Initialize();

var optimalSolution = sa.FindOptimalSolution(initialSolution);
Console.WriteLine(optimalSolution);*/

var edges = new List<Edge> {
    new Edge(1, 2, 2),
    new Edge(2, 1, 5),
    new Edge(2, 4, 4),
    new Edge(3, 5, 5),
    new Edge(5, 3, 5),
    new Edge(6, 5, 5),
    new Edge(5, 6, 5),
    new Edge(1, 3, 5),
    new Edge(3, 1, 5),
    new Edge(5, 1, -5)
};

var teamBalancer = new TeamBalancer(edges, 6);

var bestDivision = teamBalancer.GetBestTeamDivision();
Console.WriteLine("\nBest variant: ");
Console.WriteLine("Team 1: " + string.Join(", ", bestDivision.Item1));
Console.WriteLine("Team 2: " + string.Join(", ", bestDivision.Item2));

Console.WriteLine("\nAll variants: ");
var divisions = teamBalancer.GetAllTeamDivisions();
int counter = 0;
foreach (var division in divisions)
{
    //if ((++counter) % 2 == 1) continue;
    Console.WriteLine($"Team 1: {string.Join(", ", division.Team1)}, Team 2: {string.Join(", ", division.Team2)}, Total weight: {division.TotalWeight}");
}