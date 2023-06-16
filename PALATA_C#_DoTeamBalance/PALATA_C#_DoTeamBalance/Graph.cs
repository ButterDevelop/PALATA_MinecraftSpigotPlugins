using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PALATA_C__DoTeamBalance
{
    public class Graph
    {
        public List<Edge> Edges { get; set; }

        public Graph()
        {
            Edges = new List<Edge>();
        }
    }
}
