package tn.esprit.tools;

import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import javafx.scene.image.Image;
import javafx.animation.AnimationTimer;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

public class Avatar3D {

    /** Build a spinning 3D viewer pane for a GLB avatar file (from classpath /avatars/filename). */
    public static StackPane buildViewer(String filename, double width, double height) {
        StackPane holder = new StackPane();
        holder.setPrefSize(width, height);
        holder.setMinSize(width, height);

        Group modelGroup;
        try {
            modelGroup = loadGlbFromClasspath("/avatars/" + filename);
        } catch (Exception e) {
            e.printStackTrace();
            return holder;
        }

        // Center and normalize size
        javafx.geometry.Bounds b = modelGroup.getBoundsInLocal();
        double cx = (b.getMinX() + b.getMaxX()) / 2.0;
        double cy = (b.getMinY() + b.getMaxY()) / 2.0;
        double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
        double maxExtent = Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
        double targetSize = 1.8;
        double scale = maxExtent > 0 ? targetSize / maxExtent : 1.0;

        Group centered = new Group(modelGroup);
        modelGroup.getTransforms().add(0, new Translate(-cx, -cy, -cz));
        centered.setScaleX(scale);
        centered.setScaleY(-scale); // flip Y: GLTF +Y up, JavaFX +Y down
        centered.setScaleZ(scale);

        Rotate spin = new Rotate(0, Rotate.Y_AXIS);
        centered.getTransforms().add(spin);

        Group root3D = new Group();
        root3D.setDepthTest(DepthTest.ENABLE);
        root3D.getChildren().add(centered);

        AmbientLight ambient = new AmbientLight(Color.color(0.55, 0.55, 0.6));
        root3D.getChildren().add(ambient);

        PointLight key = new PointLight(Color.color(1.0, 0.95, 0.9));
        key.setTranslateX(3); key.setTranslateY(-4); key.setTranslateZ(-4);
        root3D.getChildren().add(key);

        PointLight fill = new PointLight(Color.color(0.5, 0.7, 1.0));
        fill.setTranslateX(-3); fill.setTranslateY(-2); fill.setTranslateZ(-3);
        root3D.getChildren().add(fill);

        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setNearClip(0.01);
        cam.setFarClip(100.0);
        cam.setFieldOfView(32);
        cam.setTranslateZ(-4.2);
        cam.setTranslateY(-0.05);

        SubScene subScene = new SubScene(root3D, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(cam);
        subScene.widthProperty().bind(holder.widthProperty());
        subScene.heightProperty().bind(holder.heightProperty());

        holder.getChildren().add(subScene);

        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                spin.setAngle(spin.getAngle() + 0.6);
            }
        };
        timer.start();

        return holder;
    }

    private static Group loadGlbFromClasspath(String resource) throws Exception {
        try (InputStream in = Avatar3D.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Avatar resource not found: " + resource);
            Path tmp = Files.createTempFile("avatar", ".glb");
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            GltfModelReader reader = new GltfModelReader();
            GltfModel gltf = reader.read(tmp.toUri());
            return buildGroup(gltf);
        }
    }

    private static Group buildGroup(GltfModel gltf) {
        Group group = new Group();
        for (MeshModel mesh : gltf.getMeshModels()) {
            for (MeshPrimitiveModel prim : mesh.getMeshPrimitiveModels()) {
                MeshView mv = buildMeshView(prim);
                if (mv != null) group.getChildren().add(mv);
            }
        }
        return group;
    }

    private static MeshView buildMeshView(MeshPrimitiveModel prim) {
        AccessorModel posAcc = prim.getAttributes().get("POSITION");
        if (posAcc == null) return null;
        float[] positions = readFloats(posAcc);
        if (positions == null || positions.length == 0) return null;

        AccessorModel uvAcc = prim.getAttributes().get("TEXCOORD_0");
        float[] uvs = uvAcc != null ? readFloats(uvAcc) : null;

        AccessorModel nAcc = prim.getAttributes().get("NORMAL");
        float[] normals = nAcc != null ? readFloats(nAcc) : null;

        int[] indices;
        AccessorModel idxAcc = prim.getIndices();
        if (idxAcc != null) {
            indices = readIndices(idxAcc);
        } else {
            int vertexCount = positions.length / 3;
            indices = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) indices[i] = i;
        }
        if (indices == null || indices.length < 3) return null;

        int vertexCount = positions.length / 3;
        boolean useNormals = normals != null && normals.length == vertexCount * 3;

        TriangleMesh mesh = new TriangleMesh(
                useNormals ? VertexFormat.POINT_NORMAL_TEXCOORD : VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().setAll(positions);
        if (useNormals) mesh.getNormals().setAll(normals);

        if (uvs != null && uvs.length == vertexCount * 2) {
            mesh.getTexCoords().setAll(uvs);
        } else {
            mesh.getTexCoords().setAll(new float[vertexCount * 2]);
        }

        int perVertex = useNormals ? 3 : 2;
        int[] faces = new int[indices.length * perVertex];
        for (int i = 0; i < indices.length; i++) {
            int v = indices[i];
            if (useNormals) {
                faces[i * 3] = v;
                faces[i * 3 + 1] = v;
                faces[i * 3 + 2] = v;
            } else {
                faces[i * 2] = v;
                faces[i * 2 + 1] = v;
            }
        }
        mesh.getFaces().setAll(faces);

        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        view.setMaterial(buildMaterial(prim.getMaterialModel()));
        return view;
    }

    private static PhongMaterial buildMaterial(MaterialModel materialModel) {
        PhongMaterial mat = new PhongMaterial(Color.rgb(210, 180, 160));
        mat.setSpecularColor(Color.rgb(30, 30, 35));

        if (!(materialModel instanceof MaterialModelV2)) return mat;
        MaterialModelV2 m = (MaterialModelV2) materialModel;

        float[] baseFactor = m.getBaseColorFactor();
        if (baseFactor != null && baseFactor.length >= 3) {
            mat.setDiffuseColor(Color.color(clamp01(baseFactor[0]), clamp01(baseFactor[1]), clamp01(baseFactor[2])));
        }

        TextureModel baseTex = m.getBaseColorTexture();
        Image baseImg = baseTex != null ? loadTextureImage(baseTex) : null;
        if (baseImg != null && !baseImg.isError()) {
            mat.setDiffuseMap(baseImg);
            mat.setDiffuseColor(Color.WHITE);
        }

        // ReadyPlayerMe / unlit-baked materials put the color in emissive.
        // JavaFX PhongMaterial maps this to selfIlluminationMap.
        TextureModel emissiveTex = m.getEmissiveTexture();
        Image emissiveImg = emissiveTex != null ? loadTextureImage(emissiveTex) : null;
        if (emissiveImg != null && !emissiveImg.isError()) {
            mat.setSelfIlluminationMap(emissiveImg);
            // If there's no base color texture, don't also add a base tint or
            // the emissive will be washed out. Force diffuse to black so only
            // the self-illumination texture shows (fully-lit unlit look).
            if (baseImg == null) {
                mat.setDiffuseColor(Color.BLACK);
            }
        }
        return mat;
    }

    private static Image loadTextureImage(TextureModel tex) {
        try {
            ImageModel image = tex.getImageModel();
            if (image == null) return null;
            ByteBuffer data = image.getImageData();
            if (data == null) return null;
            byte[] bytes = new byte[data.remaining()];
            ByteBuffer dup = data.duplicate();
            dup.get(bytes);
            Image img = new Image(new java.io.ByteArrayInputStream(bytes));
            if (img.isError() && img.getException() != null) img.getException().printStackTrace();
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static float[] readFloats(AccessorModel acc) {
        AccessorData data = acc.getAccessorData();
        if (data instanceof AccessorFloatData) {
            AccessorFloatData f = (AccessorFloatData) data;
            int elements = f.getNumElements();
            int components = f.getNumComponentsPerElement();
            float[] out = new float[elements * components];
            int idx = 0;
            for (int e = 0; e < elements; e++) {
                for (int c = 0; c < components; c++) {
                    out[idx++] = f.get(e, c);
                }
            }
            return out;
        }
        // Fallback: pull raw bytes and reinterpret as floats
        ByteBuffer bb = acc.getBufferViewModel().getBufferViewData().order(ByteOrder.LITTLE_ENDIAN);
        int byteOffset = acc.getByteOffset();
        int count = acc.getCount() * componentsPerType(acc.getElementType().toString());
        float[] out = new float[count];
        for (int i = 0; i < count; i++) {
            out[i] = bb.getFloat(byteOffset + i * 4);
        }
        return out;
    }

    private static int[] readIndices(AccessorModel acc) {
        ByteBuffer bb = acc.getBufferViewModel().getBufferViewData().order(ByteOrder.LITTLE_ENDIAN);
        int byteOffset = acc.getByteOffset();
        int count = acc.getCount();
        int componentType = acc.getComponentType();
        int[] out = new int[count];
        switch (componentType) {
            case 5121: // UNSIGNED_BYTE
                for (int i = 0; i < count; i++) out[i] = bb.get(byteOffset + i) & 0xFF;
                break;
            case 5123: // UNSIGNED_SHORT
                for (int i = 0; i < count; i++) out[i] = bb.getShort(byteOffset + i * 2) & 0xFFFF;
                break;
            case 5125: // UNSIGNED_INT
                for (int i = 0; i < count; i++) out[i] = bb.getInt(byteOffset + i * 4);
                break;
            default:
                return null;
        }
        return out;
    }

    private static int componentsPerType(String type) {
        switch (type) {
            case "SCALAR": return 1;
            case "VEC2": return 2;
            case "VEC3": return 3;
            case "VEC4": return 4;
            default: return 1;
        }
    }
}
