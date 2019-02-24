/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.text.MessageFormat.format;

/**
 * TODO, Move to stitch.
 * Created by covers1624 on 18/02/19.
 */
public class LineNumberRemapper {

    private final Map<String, RClass> lineMap = new HashMap<>();

    public void readMappings(File lineMappings) {
        try (BufferedReader reader = new BufferedReader(new FileReader(lineMappings))) {
            RClass clazz = null;
            String line = null;
            int i = 0;
            try {
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    String[] segs = line.trim().split("\t");
                    if (line.charAt(0) != '\t') {
                        clazz = lineMap.computeIfAbsent(segs[0], RClass::new);
                        clazz.maxLine = Integer.parseInt(segs[1]);
                        clazz.maxLineDist = Integer.parseInt(segs[2]);
                    } else {
                        clazz.lineMap.put(Integer.parseInt(segs[0]), (Integer) Integer.parseInt(segs[1]));
                    }
                    i++;
                }
            } catch (Exception e) {
                throw new RuntimeException(format("Exception reading mapping line @{0}: {1}", i, line), e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception reading LineMappings file.", e);
        }
    }

    public void process(Path input, Path output) throws IOException {
        Files.walkFileTree(input, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String rel = input.relativize(file).toString();
                Path dst = output.resolve(rel);
                Path parent = dst.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                String fName = file.getFileName().toString();
                if (fName.endsWith(".class")) {
                    if (Files.exists(dst)) {
                        Files.delete(dst);
                    }
                    String idx = rel.substring(0, rel.length() - 6);
                    int dollarPos = idx.indexOf('$');//This makes the assumption that only Java classes are to be remapped.
                    if (dollarPos >= 0) {
                        idx = idx.substring(0, dollarPos);
                    }
                    if (lineMap.containsKey(idx)) {
                        try (InputStream is = Files.newInputStream(file)) {
                            ClassReader reader = new ClassReader(is);
                            ClassWriter writer = new ClassWriter(0);

                            reader.accept(new LineNumberVisitor(Opcodes.ASM7, writer, lineMap.get(idx)), 0);
                            Files.write(dst, writer.toByteArray());
                        }
                    }

                } else {
                    Files.copy(file, dst, StandardCopyOption.REPLACE_EXISTING);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static class LineNumberVisitor extends ClassVisitor {

        private final RClass rClass;

        public LineNumberVisitor(int api, ClassVisitor classVisitor, RClass rClass) {
            super(api, classVisitor);
            this.rClass = rClass;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                @Override
                public void visitLineNumber(int line, Label start) {
                    int tLine = line;
                    if (tLine <= 0) {
                        super.visitLineNumber(line, start);
                    } else if (tLine >= rClass.maxLine) {
                        super.visitLineNumber(rClass.maxLineDist, start);
                    } else {
                        Integer matchedLine = null;
                        while (tLine <= rClass.maxLine && ((matchedLine = rClass.lineMap.get(tLine)) != null)) {
                            tLine++;
                        }
                        super.visitLineNumber(matchedLine != null ? matchedLine : rClass.maxLineDist, start);
                    }
                }
            };
        }
    }

    private static class RClass {

        private final String name;
        private int maxLine;
        private int maxLineDist;
        //Int2ObjectMap, avoid unnecessary boxing / unboxing.
        private Int2ObjectMap<Integer> lineMap = new Int2ObjectOpenHashMap<>();

        private RClass(String name) {
            this.name = name;
        }
    }

}
