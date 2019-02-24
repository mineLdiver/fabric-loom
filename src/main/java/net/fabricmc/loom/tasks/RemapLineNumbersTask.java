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

package net.fabricmc.loom.tasks;

import net.fabricmc.loom.tasks.cache.CachedInput;
import net.fabricmc.loom.tasks.cache.CachedInputTask;
import net.fabricmc.loom.util.LineNumberRemapper;
import net.fabricmc.stitch.util.StitchUtil;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

/**
 * Created by covers1624 on 18/02/19.
 */
public class RemapLineNumbersTask extends CachedInputTask {

    private Object input;
    private Object output;
    private Object lineMap;

    @TaskAction
    public void doTask() throws IOException {
        LineNumberRemapper remapper = new LineNumberRemapper();
        remapper.readMappings(getLineMap());
        try (StitchUtil.FileSystemDelegate inFs = StitchUtil.getJarFileSystem(getInput(), true)) {
            try (StitchUtil.FileSystemDelegate outFs = StitchUtil.getJarFileSystem(getOutput(), true)) {
                remapper.process(inFs.get().getPath("/"), outFs.get().getPath("/"));
            }
        }
    }

    //@formatter:off
    @CachedInput public File getInput() { return getProject().file(input); }
    @OutputFile public File getOutput() { return getProject().file(output); }
    @CachedInput public File getLineMap() { return getProject().file(lineMap); }
    public void setInput(Object input) { this.input = input; }
    public void setOutput(Object output) { this.output = output; }
    public void setLineMap(Object lineMap) { this.lineMap = lineMap; }
    //@foramtter:on
}
