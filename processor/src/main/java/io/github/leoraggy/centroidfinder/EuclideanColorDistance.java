    package io.github.leoraggy.centroidfinder;
    public class EuclideanColorDistance implements ColorDistanceFinder {
        /**
         * Returns the euclidean color distance between two hex RGB colors.
         * 
         * Each color is represented as a 24-bit integer in the form 0xRRGGBB, where
         * RR is the red component, GG is the green component, and BB is the blue component,
         * each ranging from 0 to 255.
         * 
         * The Euclidean color distance is calculated by treating each color as a point
         * in 3D space (red, green, blue) and applying the Euclidean distance formula:
         * 
         * sqrt((r1 - r2)^2 + (g1 - g2)^2 + (b1 - b2)^2)
         * 
         * This gives a measure of how visually different the two colors are.
         * 
         * @param colorA the first color as a 24-bit hex RGB integer
         * @param colorB the second color as a 24-bit hex RGB integer
         * @return the Euclidean distance between the two colors
         */
        @Override
        public double distance(int colorA, int colorB) {
            return hexToRGB(colorA, colorB);
        }

        private static double hexToRGB(int colorA, int colorB) {
            // Grab the r1 b1 g1 (colorA)
            double r1 = (colorA >> 16) & 0xFF;
            double g1 = (colorA >> 8) & 0xFF;
            double b1 = colorA & 0xFF;

            // Grab the r2 b2 g2 (colorB)
            double r2 = (colorB >> 16) & 0xFF;
            double g2 = (colorB >> 8) & 0xFF;
            double b2 = colorB & 0xFF;

            // Euclidean Equation
            // subtract
            double rValue = r1 - r2;
            double gValue = g1 - g2;
            double bValue = b1 - b2;

            // square
            rValue = rValue * rValue;
            gValue = gValue * gValue;
            bValue = bValue * bValue;

            // sum
            double sumValue = rValue + gValue + bValue;

            // sqaure
            return Math.sqrt(sumValue);
        }
    }
