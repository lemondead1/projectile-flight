#version 110

uniform mat4 transformMatrix;

attribute vec3 position;
attribute vec4 color;

varying vec4 vertColor;

void main() {
    vertColor = color;
    gl_Position = transformMatrix * vec4(position, 1);
}
