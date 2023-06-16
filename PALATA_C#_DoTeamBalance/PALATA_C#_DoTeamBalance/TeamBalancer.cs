using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PALATA_C__DoTeamBalance
{
    public class TeamDivision
    {
        public List<int> Team1 { get; set; }
        public List<int> Team2 { get; set; }
        public int TotalWeight { get; set; }
    }

    public class TeamBalancer
    {
        private List<Edge> edges;
        private int numberOfPlayers;

        public TeamBalancer(List<Edge> edges, int numberOfPlayers)
        {
            this.edges = edges;
            this.numberOfPlayers = numberOfPlayers;
        }

        // Метод для создания списка всех подмножеств указанного размера
        private IEnumerable<IEnumerable<T>> Combinations<T>(IEnumerable<T> elements, int length)
        {
            for (int i = 0; i < elements.Count(); i++)
            {
                if (length == 1)
                    yield return new T[] { elements.ElementAt(i) };
                else
                {
                    foreach (var next in Combinations(elements.Skip(i + 1), length - 1))
                        yield return new T[] { elements.ElementAt(i) }.Concat(next);
                }
            }
        }

        // Метод для получения веса команды
        private int GetTeamWeight(List<int> team)
        {
            int weight = 0;
            foreach (var edge in edges)
            {
                if (team.Contains(edge.From) && team.Contains(edge.To))
                {
                    weight += edge.Weight;
                }
            }
            return weight;
        }

        // Метод для получения лучшего разделения на команды
        public (List<int>, List<int>) GetBestTeamDivision()
        {
            var players = Enumerable.Range(1, numberOfPlayers).ToList();
            var bestDivision = (Team1: new List<int>(), Team2: new List<int>());
            var maxWeight = int.MinValue;

            foreach (var team1 in Combinations(players, numberOfPlayers / 2))
            {
                var team1List = team1.ToList();
                var team2List = players.Except(team1List).ToList();

                var team1Weight = GetTeamWeight(team1List);
                var team2Weight = GetTeamWeight(team2List);
                var divisionWeight = team1Weight + team2Weight;

                if (divisionWeight > maxWeight)
                {
                    maxWeight = divisionWeight;
                    bestDivision = (Team1: team1List, Team2: team2List);
                }
            }
            return bestDivision;
        }

        public List<TeamDivision> GetAllTeamDivisions()
        {
            var players = Enumerable.Range(1, numberOfPlayers).ToList();
            var allDivisions = new List<TeamDivision>();

            foreach (var team1 in Combinations(players, numberOfPlayers / 2))
            {
                var team1List = team1.ToList();
                var team2List = players.Except(team1List).ToList();

                var team1Weight = GetTeamWeight(team1List);
                var team2Weight = GetTeamWeight(team2List);
                var divisionWeight = team1Weight + team2Weight;

                allDivisions.Add(new TeamDivision
                {
                    Team1 = team1List,
                    Team2 = team2List,
                    TotalWeight = divisionWeight
                });
            }

            allDivisions.Sort((d1, d2) => d1.TotalWeight.CompareTo(d2.TotalWeight));
            return allDivisions;
        }
    }
}
