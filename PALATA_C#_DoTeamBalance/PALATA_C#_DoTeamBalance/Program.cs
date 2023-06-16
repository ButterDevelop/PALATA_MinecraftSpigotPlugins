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
    new Edge(1, 2, 5),
    new Edge(1, 3, 5),
    new Edge(1, 4, 5),
    new Edge(1, 6, 5),
    new Edge(1, 7, 5),
    new Edge(1, 8, 5),
    new Edge(1, 11, 5),
    new Edge(1, 12, 5),
    new Edge(1, 15, 5),
    new Edge(1, 16, 5),
    new Edge(1, 18, 5),
    new Edge(11, 1, 5),
    new Edge(11, 8, 5),
    new Edge(11, 15, 5),
    new Edge(11, 2, 5),
    new Edge(11, 12, 5),
    new Edge(11, 18, 5),
    new Edge(11, 6, 5),
    new Edge(18, 1, 5),
    new Edge(18, 20, 5),
    new Edge(18, 11, 5),
    new Edge(18, 8, 5),
    new Edge(15, 17, 5),
    new Edge(3, 4, 5),
    new Edge(3, 7, 5),
    new Edge(3, 16, 5),
    new Edge(3, 1, 5),
    new Edge(3, 21, 5),
    new Edge(3, 15, 5),
    new Edge(2, 1, 5),
    new Edge(2, 5, 5),
    new Edge(2, 8, 5),
    new Edge(2, 11, 5),
    new Edge(2, 15, 5),
    new Edge(2, 17, 5),
    new Edge(7, 3, 5),
    new Edge(7, 4, 5),
    new Edge(16, 1, 5),
    new Edge(16, 3, 5),
    new Edge(16, 21, 5),
    new Edge(21, 3, 5),
    new Edge(21, 16, 5),
    new Edge(10, 1, 5),
    new Edge(10, 8, 5),
    new Edge(12, 1, 5),
    new Edge(12, 8, 5),
    new Edge(12, 11, 5),
    new Edge(12, 15, 5),
    new Edge(12, 20, 5),
    new Edge(19, 3, 5),
    new Edge(19, 4, 5),
    new Edge(19, 17, 5),
    new Edge(17, 3, 5),
    new Edge(17, 4, 5),
    new Edge(17, 15, 5),
    new Edge(17, 19, 5),
    new Edge(14, 1, 5),
    new Edge(14, 4, 5),
    new Edge(14, 19, 5),
    new Edge(14, 3, 5),
    new Edge(20, 12, 5),
    new Edge(20, 18, 5),
    new Edge(8, 1, 5),
    new Edge(8, 2, 5),
    new Edge(8, 10, 5),
    new Edge(8, 11, 5),
    new Edge(8, 12, 5),
    new Edge(6, 1, 5),
    new Edge(6, 11, 5),
    new Edge(1, 10, -5),
    new Edge(1, 14, -5),
    new Edge(1, 20, -5),
    new Edge(1, 21, -5),
    new Edge(1, 22, -5),
    new Edge(1, 23, -5),
    new Edge(11, 4, -5),
    new Edge(11, 10, -5),
    new Edge(11, 14, -5),
    new Edge(11, 17, -5),
    new Edge(11, 19, -5),
    new Edge(11, 12, -5),
    new Edge(11, 21, -5),
    new Edge(11, 22, -5),
    new Edge(11, 23, -5),
    new Edge(15, 9, -5),
    new Edge(15, 13, -5),
    new Edge(15, 14, -5),
    new Edge(15, 21, -5),
    new Edge(15, 22, -5),
    new Edge(15, 23, -5),
    new Edge(2, 10, -5),
    new Edge(2, 21, -5),
    new Edge(2, 14, -5),
    new Edge(2, 7, -5),
    new Edge(2, 22, -5),
    new Edge(12, 5, -5),
    new Edge(12, 6, -5),
    new Edge(12, 9, -5),
    new Edge(12, 10, -5),
    new Edge(12, 13, -5),
    new Edge(12, 14, -5),
    new Edge(12, 21, -5),
    new Edge(8, 9, -5),
    new Edge(8, 4, -5)
};

var teamBalancer = new TeamBalancer(edges, 23);

var bestDivision = teamBalancer.GetBestTeamDivision();
Console.WriteLine("\nBest variant: ");
Console.WriteLine("Team 1: " + string.Join(", ", bestDivision.Item1));
Console.WriteLine("Team 2: " + string.Join(", ", bestDivision.Item2));

Console.WriteLine("\nAll variants: ");
var divisions = teamBalancer.GetAllTeamDivisions();
int counter = 0;
string result = "";
Console.WriteLine($"Divisions count: {divisions.Count}");
foreach (var division in divisions)
{
    if ((++counter) % 1000 == 1) Console.WriteLine(counter);
    if (division.TotalWeight < 200) continue;
    //if ((++counter) % 2 == 1) continue;
    string s = $"Team 1: {string.Join(", ", division.Team1)}, Team 2: {string.Join(", ", division.Team2)}, Total weight: {division.TotalWeight}";
    //Console.WriteLine(s);
    result += s + Environment.NewLine;
}
File.WriteAllText("teams.txt", result);