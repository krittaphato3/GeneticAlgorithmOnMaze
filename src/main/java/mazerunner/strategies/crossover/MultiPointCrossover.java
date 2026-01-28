package mazerunner.strategies.crossover;

import mazerunner.models.Walker;
import java.util.Arrays;
import java.util.Random;

public class MultiPointCrossover implements CrossOverStrategy {

    private Random random = new Random();
    private double crossoverRate;
    private int numPoints;

    public MultiPointCrossover(double crossoverRate, int numPoints) {
        this.crossoverRate = crossoverRate;
        this.numPoints = numPoints;
    }

    @Override
    public Walker[] crossover(Walker parent1, Walker parent2) {
        int[] p1Genes = parent1.getGenes();
        int[] p2Genes = parent2.getGenes();
        int length = p1Genes.length;

        int[] c1Genes = new int[length];
        int[] c2Genes = new int[length];

        if (random.nextDouble() <= crossoverRate) {
            // สุ่มจุดตัด
            int[] cutPoints = new int[numPoints];
            for (int i = 0; i < numPoints; i++) {
                cutPoints[i] = random.nextInt(length - 1) + 1;
            }
            Arrays.sort(cutPoints);

            boolean swap = false;  // เริ่มต้นที่พ่อยังไม่ต้องสลับ
            int currentPointIndex = 0; // ตัวบอกว่าถึงจุดตันที่เท่าไหร่

            for (int i = 0; i < length; i++) {
                if (currentPointIndex < numPoints && i == cutPoints[currentPointIndex]) {
                    // เจอจุดตัดก็สลับ
                    swap = !swap;
                    currentPointIndex++;
                }
                // ใส่ยีนให้ลูก
                if (!swap) {
                    // เอาของเดิมไปใส่
                    c1Genes[i] = p1Genes[i];
                    c2Genes[i] = p2Genes[i];
                } else {
                    // เอาที่สลับฝั่ง
                    c1Genes[i] = p2Genes[i];
                    c2Genes[i] = p1Genes[i];
                }
            }
        } else {
            // ไม่เกิดการ crossover
            c1Genes = p1Genes.clone();
            c2Genes = p2Genes.clone();
        }

        Walker child1 = new Walker(c1Genes);
        Walker child2 = new Walker(c2Genes);

        Walker[] children = new Walker[2];
        children[0] = child1;
        children[1] = child2;

        return children;
    }
}
