package ru.chernevich;

import org.joml.Matrix4f;
import org.lwjgl.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;

    private List<Cube> cubes = new ArrayList<>(); //список кубиков
    private Cube currentCube; //текущий кубик
    private boolean rotating = false; //переменная для вращения кубика

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init(); //инициируем окно
        loop(); //рисуем в окне

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(1200, 800, "3D-Game", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if(action == GLFW_PRESS || key == GLFW_RELEASE) {
                switch (key) {
                    case GLFW_KEY_UP -> currentCube.y += 0.05f;
                    case GLFW_KEY_DOWN -> currentCube.y -= 0.05f;
                    case GLFW_KEY_LEFT -> currentCube.x -= 0.05f;
                    case GLFW_KEY_RIGHT -> currentCube.x += 0.05f;
                    case GLFW_KEY_W -> currentCube.z += 0.05f;
                    case GLFW_KEY_S -> currentCube.z -= 0.05f;
                    case GLFW_KEY_A -> rotating = !rotating;
                    case GLFW_KEY_ENTER -> {
                        cubes.add(currentCube); // добавляем кубик
                        currentCube = new Cube(0, 0, -5, 0);
                    }
                }
            }

            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); //закрываем приложение
        });

        currentCube = new Cube(0, 0, -5, 0);

        for(int row = 0; row < 6; row++) {
            float z = 4.1f - row * 0.3f; // z координата кубика
            for (int i = 0; i < 25; i++) {
                float x = -3.4f + i * 0.3f; // x координата кубика
                cubes.add(new Cube(x, -1.7f, z, 0.0f)); // добавляем кубик
            }
        }

        // стек памяти
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2, // вертикальное смещение
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);

        glfwSwapInterval(1);

        glfwShowWindow(window);
    }

    private void gradientDg() {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, 1200, 0, 800, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity(); // сбрасываем матрицу

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_DST_ALPHA);

        glDepthMask(false);

        glBegin(GL_QUADS);

        glColor3f(0.0f, 0.3f, 0.7f);
        glVertex2f(0.0f, 0.0f);
        glVertex2f(1200.0f, 0.0f);

        glColor3f(0.0f, 0.3f, 0.7f);
        glVertex2f(1200.0f, 800.0f);
        glVertex2f(0.0f, 800.0f);

        glEnd();

        glDepthMask(true);
        glDisable(GL_BLEND);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void loop() {
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST); // включаем глубину
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f),
                1200.0f / 800.0f, 0.1f, 100f);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16); // буфер для матриц
        projection.get(floatBuffer); // получаем матрицу

        glClearColor(0f, 0f, 0f, 0f);

        // цикл отрисовки
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // очистка буфера глубины

            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(floatBuffer); // получаем матрицу

            gradientDg(); // отрисовка градиента

            for (Cube cube : cubes) {
                drownCube(cube); // отрисовка кубика
            }

            drownCube(currentCube); // отрисовка текущего кубика.

            if (rotating) {
                currentCube.angle += 0.5f;
            }

            glfwSwapBuffers(window); // переключаем буферы глубины
        }
    }

    private void drownCube(Cube cube) {

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslatef(cube.x, cube.y, cube.z);
        glRotatef(cube.angle, 0f, 1f, 0f);
        glScalef(0.33f, 0.33f,0.33f);

        glBegin(GL_QUADS);
        // лицевые координаты
        glColor3f(79/255f, 171/255f, 67/255f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);

        // задние координаты
        glColor3f(79/255f, 171/255f, 67/255f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);

        // верхние координаты
        glColor3f(36/255f, 79/255f, 31/255f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);

        // нижние координаты
        glColor3f(36/255f, 79/255f, 31/255f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);

        // правые координаты
        glColor3f(36/255f, 79/255f, 31/255f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);

        // левые координаты
        glColor3f(36/255f, 79/255f, 31/255f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glEnd(); // закрываем окно

    }

    public static void main(String[] args) {
        new Main().run();
    }

}