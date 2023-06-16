using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PALATA_C__DoTeamBalance
{
    public class SimulatedAnnealing
    {
        private double temperature;
        private double coolingRate;
        private Random random;

        public SimulatedAnnealing(double initialTemperature, double coolingRate)
        {
            this.temperature = initialTemperature;
            this.coolingRate = coolingRate;
            this.random = new Random();
        }

        public Solution FindOptimalSolution(Solution initialSolution)
        {
            Solution currentSolution = initialSolution;
            Solution bestSolution = (Solution)currentSolution.Clone();

            while (temperature > 1)
            {
                // Создаем новое решение
                Solution newSolution = (Solution)currentSolution.Clone();
                newSolution.Mutate();

                // Вычисляем стоимость обоих решений
                double currentEnergy = currentSolution.CalculateCost();
                double neighbourEnergy = newSolution.CalculateCost();

                // Если новое решение лучше или ему разрешено быть хуже с определенной вероятностью
                if (AcceptanceProbability(currentEnergy, neighbourEnergy) > random.NextDouble())
                    currentSolution = newSolution;

                // Сохраняем наилучшее решение
                if (currentSolution.CalculateCost() < bestSolution.CalculateCost())
                    bestSolution = (Solution)currentSolution.Clone();

                // Охлаждаем систему
                temperature *= 1 - coolingRate;
            }

            return bestSolution;
        }

        // Вероятность принятия решения
        private double AcceptanceProbability(double currentEnergy, double newEnergy)
        {
            if (newEnergy < currentEnergy)
                return 1.0;
            else
                return Math.Exp((currentEnergy - newEnergy) / temperature);
        }
    }
}
