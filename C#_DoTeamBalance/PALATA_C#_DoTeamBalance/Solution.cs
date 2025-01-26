using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PALATA_C__DoTeamBalance
{
    public class Solution
    {
        public List<int> Team1 { get; set; }
        public List<int> Team2 { get; set; }
        private Graph _graph;

        public Solution(Graph graph)
        {
            _graph = graph;
            Team1 = new List<int>();
            Team2 = new List<int>();
        }

        public void Initialize()
        {
            // инициализируем случайным образом разбиение на команды
            var players = _graph.Edges.SelectMany(e => new[] { e.From, e.To }).Distinct().ToList();
            foreach (var player in players)
            {
                if (new Random().NextDouble() >= 0.5)
                    Team1.Add(player);
                else
                    Team2.Add(player);
            }
        }

        public void Mutate()
        {
            // перемещаем случайного игрока из одной команды в другую
            if (Team1.Any() && Team2.Any())
            {
                if (new Random().NextDouble() >= 0.5)
                {
                    var player = Team1[new Random().Next(Team1.Count)];
                    Team1.Remove(player);
                    Team2.Add(player);
                }
                else
                {
                    var player = Team2[new Random().Next(Team2.Count)];
                    Team2.Remove(player);
                    Team1.Add(player);
                }
            }
        }

        public double CalculateCost()
        {
            double cost = 0;

            foreach (var edge in _graph.Edges)
            {
                if (Team1.Contains(edge.From) && Team1.Contains(edge.To) ||
                    Team2.Contains(edge.From) && Team2.Contains(edge.To))
                {
                    cost += Math.Abs(edge.Weight);
                }
            }

            return cost;
        }

        public Solution Clone()
        {
            var clonedSolution = new Solution(_graph)
            {
                Team1 = new List<int>(Team1),
                Team2 = new List<int>(Team2)
            };

            return clonedSolution;
        }

        public override string ToString()
        {
            var builder = new StringBuilder();

            builder.AppendLine("Team 1:");
            foreach (var player in Team1)
            {
                builder.AppendLine($"Player {player}");
            }

            builder.AppendLine("Team 2:");
            foreach (var player in Team2)
            {
                builder.AppendLine($"Player {player}");
            }

            return builder.ToString();
        }
    }
}
