package com.wetpants.mydoublependulum;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.Log;

public class Pendulum {

    private double phi = 0;
    private int width;
    private int height;
    private int number_nodes;
    private int size_nodes;
    private Node[] nodes;
    private int color_background;
    private double g;
    private double dt;
    private Node origin;

    public Pendulum(int width, int height, int[] colors) {
        this.width = width;
        this.height = height;

        size_nodes = 75;
        number_nodes = colors.length;
        g = 9.8;
        dt = 0.015;
        color_background = Color.TRANSPARENT;

        origin = new Node(Color.GRAY, 2, 4, null);
        nodes = new Node[number_nodes];
        nodes[0] = new Node(colors[0], 0, -1, origin);
        for (int i = 1; i < number_nodes; i++)
            nodes[i] = new Node(colors[i],
                    0, -1.0/(number_nodes - 1),
                    nodes[i-1]);
    }

    public void update() {
        if (number_nodes == 2)
            updateRungeKutta();
        else
            updateAny();
    }

    public void updateAny(){

    }

    public void updateEuler() {
        double ddt_omega1 = calculateDDtOmega1(nodes[0].m, nodes[0].l,
                nodes[0].phi, nodes[0].omega,
                nodes[1].m, nodes[1].l,
                nodes[1].phi, nodes[1].omega);
        double ddt_omega2 = calculateDDtOmega2(nodes[0].m, nodes[0].l,
                nodes[0].phi, nodes[0].omega,
                nodes[1].m, nodes[1].l,
                nodes[1].phi, nodes[1].omega);
        nodes[0].omega += dt * ddt_omega1;
        nodes[0].phi += dt * nodes[0].omega;
        nodes[0].x = origin.x + Math.sin(nodes[0].phi) * nodes[0].l;
        nodes[0].y = origin.y - Math.cos(nodes[0].phi) * nodes[0].l;
        nodes[1].omega += dt * ddt_omega2;
        nodes[1].phi += dt * nodes[1].omega;
        nodes[1].x = nodes[0].x + Math.sin(nodes[1].phi) * nodes[1].l;
        nodes[1].y = nodes[0].y - Math.cos(nodes[1].phi) * nodes[1].l;
    }

    public void updateRungeKutta() {
/*
        double forceX = dx / (dt + (System.currentTimeMillis() - Math.max(0, touchEnd)));
        double forceY = dy / (dt + (System.currentTimeMillis() - Math.max(0, touchEnd)));
        double dx1 = nodes[0].x - origin.x;
        double dx2 = nodes[1].x - nodes[0].x;
        double dy1 = nodes[0].y - origin.y;
        double dy2 = nodes[1].y - nodes[0].y;
        double multi = 100;
        double force1 = (dy_force * dx1 - dx_force * dy1) / multi;
        double force2 = (dy_force * dx2 - dx_force * dy2) / multi;
        Log.d("FORCES_TEST", "updateEuler: " + force1 + " " + force2);*/

        double[] alpha = {1.0, 2.0, 2.0, 1.0};
        double sumO1 = 0, sumO2 = 0, do1 = 0, do2 = 0;
        for (double a : alpha) {
            double ddt_omega1 = calculateDDtOmega1(nodes[0].m, nodes[0].l,
                    nodes[0].phi, nodes[0].omega + dt * do1 / a,
                    nodes[1].m, nodes[1].l,
                    nodes[1].phi, nodes[1].omega + dt * do2 / a);
            double ddt_omega2 = calculateDDtOmega2(nodes[0].m, nodes[0].l,
                    nodes[0].phi, nodes[0].omega + dt * do1 / a,
                    nodes[1].m, nodes[1].l,
                    nodes[1].phi, nodes[1].omega + dt * do2 / a);
            sumO1 += a * dt * ddt_omega1 / 6;
            sumO2 += a * dt * ddt_omega2 / 6;
            do1 = ddt_omega1;
            do2 = ddt_omega2;
        }
        nodes[0].omega += sumO1;
        nodes[1].omega += sumO2;

        nodes[0].phi += dt * nodes[0].omega;
        nodes[1].phi += dt * nodes[1].omega;

        nodes[0].updateXY();
        nodes[1].updateXY();
    }

    public double calculateDDtOmega1(double m1, double l1, double p1, double o1,
                                     double m2, double l2, double p2, double o2) {
        double ddt_omega1 = (-g * (2 * m1 + m1) * Math.sin(p1) - m2 * g * Math.sin(p1 - 2 * p2)
                - 2 * Math.sin(p1 - p2) * m2 * (Math.pow(o2, 2) * l2 + Math.pow(l2, 2) * l1 * Math.cos(o1 - o2)))
                / (l1 * (2 * m1 + m1 - m2 * Math.cos(2 * p1 - 2 * p2)));
        return ddt_omega1;
    }

    public double calculateDDtOmega2(double m1, double l1, double p1, double o1,
                                     double m2, double l2, double p2, double o2) {
        double ddt_omega2 = 2 * Math.sin(p1 - p2) *
                (Math.pow(o1, 2) * l1 * (m1 + m2) + g * (m1 + m2) * Math.cos(p1)
                        + Math.pow(o2, 2) * l2 * m2 * Math.cos(p1 - p2))
                / (l2 * (2 * m1 + m2 - m2 * Math.cos(2 * p1 - 2 * p2)));
        return ddt_omega2;
    }

    public void draw(Canvas canvas) {
        origin.draw(canvas);
        for (Node node : nodes) {
            node.draw(canvas);
        }
    }

    public int closestNode(int x, int y) {
        double dist = dist(x, nodes[0].xOnScreen(), y, nodes[0].yOnScreen());
        int closest = 0;
        for (int i = 1; i < nodes.length; i++) {
            double distNew = dist(x, nodes[i].xOnScreen(), y, nodes[i].yOnScreen());
            if (distNew < dist) {
                dist = distNew;
                closest = i;
            }
        }
        return closest;
    }

    public void lightUp(int i) {
        nodes[i].lightUp();
    }

    public void lightNormal(int i) {
        nodes[i].lightNormal();
    }

    private double dist(int x1, int x2, int y1, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public void moveNode(int touchedNode, float x, float y) {
        nodes[touchedNode].setPhi(x, y);
        for (Node node : nodes) {
            node.updateXY();
            node.omega = 0;
        }
    }

    private class Node {
        private int color;
        public double x;
        public double y;
        private double m, l, phi, omega;
        private float size;
        private Node prev;

        public Node(int color, double x, double y, Node prev) {
            this.color = color;
            m = 2.0;
            omega = 0;
            this.prev = prev;
            size = (float) size_nodes;

            if (prev != null) {
                this.x = x + prev.x;
                this.y = y + prev.y;
                l = Math.sqrt(x * x + y * y);
                // phi = Math.PI / 2;
                phi = Math.atan(-x / y);
            } else {
                this.x = x;
                this.y = y;
            }
        }

        public void lightUp() {
            size = (float) size_nodes * 1.25f;
        }

        public void lightNormal() {
            size = (float) size_nodes;
        }

        public void setPhi(float xOnScreen, float yOnScreen) {
            double x = xOnField(xOnScreen);
            double y = yOnField(yOnScreen);
            this.x = x;
            this.y = y;
            double dx = x - prev.x;
            double dy = y - prev.y;
            phi = Math.atan(-dx / dy);
            if (dy > 0) {
                    phi += Math.PI;
            }
        }

        private double xOnField(float xOnScreen) {
            return (xOnScreen - size_nodes) * 4.5 / width;
        }

        private double yOnField(float yOnScreen) {
            return (height - yOnScreen - size_nodes) * 4.5 / width;
        }

        public void updateXY() {
            x = prev.x + Math.sin(phi) * l;
            y = prev.y - Math.cos(phi) * l;
        }

        public int xOnScreen() {
            return (int) (x * width / 4.5) + size_nodes;
        }

        public int yOnScreen() {
            return height - ((int) (y * width / 4.5) + size_nodes);
        }

        public void draw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(color);
            if (prev != null) {
                canvas.drawLine(xOnScreen(), yOnScreen(), prev.xOnScreen(), prev.yOnScreen(), paint);
            }
            RadialGradient radialGradient = new RadialGradient(xOnScreen(), yOnScreen(), size_nodes, color, color_background, Shader.TileMode.MIRROR);
            Paint brush = new Paint();
            brush.setShader(radialGradient);
            canvas.drawCircle(xOnScreen(), yOnScreen(), size, brush);
        }
    }
}
