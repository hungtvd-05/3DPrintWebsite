package com.web.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class STLPreviewService {
    
    private String uploadPath = System.getProperty("user.dir") + File.separator
            + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator + "product" + File.separator;
    
    public String generatePreview(MultipartFile stlFile, String previewFileName) throws IOException {
//        String previewFileName = UUID.randomUUID().toString() + "_preview.png";
        String previewPath = uploadPath + "stl_preview" + File.separator + previewFileName;
        
        // Tạo thư mục nếu chưa tồn tại
        Files.createDirectories(Paths.get(uploadPath + "stl_preview"));
        
        // Parse STL file và tạo preview
        STLModel model = parseSTL(stlFile.getInputStream());
        BufferedImage preview = generatePreviewImage(model);
        
        // Lưu preview image
        Files.createDirectories(Paths.get(previewPath).getParent());
        ImageIO.write(preview, "PNG", new File(previewPath));
        
        return previewFileName;
    }
    
    private STLModel parseSTL(InputStream inputStream) throws IOException {
        byte[] header = new byte[80];
        inputStream.read(header);
        
        // Đọc số lượng triangles
        byte[] triangleCountBytes = new byte[4];
        inputStream.read(triangleCountBytes);
        int triangleCount = ByteBuffer.wrap(triangleCountBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        
        List<Triangle> triangles = new ArrayList<>();
        
        for (int i = 0; i < triangleCount; i++) {
            Triangle triangle = readTriangle(inputStream);
            triangles.add(triangle);
        }
        
        return new STLModel(triangles);
    }
    
    private Triangle readTriangle(InputStream inputStream) throws IOException {
        // Đọc normal vector (3 floats)
        Vector3D normal = readVector3D(inputStream);
        
        // Đọc 3 vertices
        Vector3D v1 = readVector3D(inputStream);
        Vector3D v2 = readVector3D(inputStream);
        Vector3D v3 = readVector3D(inputStream);
        
        // Skip attribute byte count
        inputStream.skip(2);
        
        return new Triangle(normal, v1, v2, v3);
    }
    
    private Vector3D readVector3D(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[12]; // 3 floats * 4 bytes
        inputStream.read(buffer);
        
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        float x = bb.getFloat();
        float y = bb.getFloat();
        float z = bb.getFloat();
        
        return new Vector3D(x, y, z);
    }
    
    private BufferedImage generatePreviewImage(STLModel model) {
        int width = 400;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Calculate bounds
        Bounds bounds = model.getBounds();
        
        // Calculate scale và offset để fit trong image
        double scaleX = (width * 0.8) / (bounds.maxX - bounds.minX);
        double scaleY = (height * 0.8) / (bounds.maxY - bounds.minY);
        double scale = Math.min(scaleX, scaleY);
        
        double offsetX = width / 2.0 - (bounds.maxX + bounds.minX) * scale / 2.0;
        double offsetY = height / 2.0 - (bounds.maxY + bounds.minY) * scale / 2.0;
        
        // Draw triangles
        g2d.setColor(Color.GRAY);
        for (Triangle triangle : model.getTriangles()) {
            drawTriangle(g2d, triangle, scale, offsetX, offsetY);
        }
        
        g2d.dispose();
        return image;
    }
    
    private void drawTriangle(Graphics2D g2d, Triangle triangle, double scale, double offsetX, double offsetY) {
        // Simple orthographic projection (top view)
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        
        xPoints[0] = (int) (triangle.v1.x * scale + offsetX);
        yPoints[0] = (int) (triangle.v1.y * scale + offsetY);
        
        xPoints[1] = (int) (triangle.v2.x * scale + offsetX);
        yPoints[1] = (int) (triangle.v2.y * scale + offsetY);
        
        xPoints[2] = (int) (triangle.v3.x * scale + offsetX);
        yPoints[2] = (int) (triangle.v3.y * scale + offsetY);
        
        g2d.drawPolygon(xPoints, yPoints, 3);
    }
    
    // Helper classes
    public static class STLModel {
        private List<Triangle> triangles;
        
        public STLModel(List<Triangle> triangles) {
            this.triangles = triangles;
        }
        
        public List<Triangle> getTriangles() {
            return triangles;
        }
        
        public Bounds getBounds() {
            if (triangles.isEmpty()) {
                return new Bounds(0, 0, 0, 0, 0, 0);
            }
            
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;
            
            for (Triangle triangle : triangles) {
                for (Vector3D vertex : new Vector3D[]{triangle.v1, triangle.v2, triangle.v3}) {
                    minX = Math.min(minX, vertex.x);
                    minY = Math.min(minY, vertex.y);
                    minZ = Math.min(minZ, vertex.z);
                    maxX = Math.max(maxX, vertex.x);
                    maxY = Math.max(maxY, vertex.y);
                    maxZ = Math.max(maxZ, vertex.z);
                }
            }
            
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
    
    public static class Triangle {
        public Vector3D normal, v1, v2, v3;
        
        public Triangle(Vector3D normal, Vector3D v1, Vector3D v2, Vector3D v3) {
            this.normal = normal;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }
    }
    
    public static class Vector3D {
        public float x, y, z;
        
        public Vector3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    public static class Bounds {
        public float minX, minY, minZ, maxX, maxY, maxZ;
        
        public Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }
}