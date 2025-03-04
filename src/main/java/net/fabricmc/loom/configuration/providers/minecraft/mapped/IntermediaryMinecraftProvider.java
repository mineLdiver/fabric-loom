/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 FabricMC
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

package net.fabricmc.loom.configuration.providers.minecraft.mapped;

import java.util.List;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.providers.minecraft.MergedMinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftJar;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.SingleJarEnvType;
import net.fabricmc.loom.configuration.providers.minecraft.SingleJarMinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.SplitMinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.GluedMinecraftProvider;
import net.fabricmc.loom.util.SidedClassVisitor;
import net.fabricmc.tinyremapper.TinyRemapper;

public abstract sealed class IntermediaryMinecraftProvider<M extends MinecraftProvider> extends AbstractMappedMinecraftProvider<M> permits IntermediaryMinecraftProvider.GluedImpl, IntermediaryMinecraftProvider.MergedImpl, IntermediaryMinecraftProvider.SingleJarImpl, IntermediaryMinecraftProvider.SplitImpl {
	public IntermediaryMinecraftProvider(ConfigContext configContext, M minecraftProvider) {
		super(configContext, minecraftProvider);
	}

	@Override
	public final MappingsNamespace getTargetNamespace() {
		return MappingsNamespace.INTERMEDIARY;
	}

	@Override
	public MavenScope getMavenScope() {
		return MavenScope.GLOBAL;
	}

	public static final class MergedImpl extends IntermediaryMinecraftProvider<MergedMinecraftProvider> implements Merged {
		public MergedImpl(ConfigContext configContext, MergedMinecraftProvider minecraftProvider) {
			super(configContext, minecraftProvider);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
				new RemappedJars(minecraftProvider.getMergedJar(), getMergedJar(), MappingsNamespace.OFFICIAL)
			);
		}
	}

	public static final class SplitImpl extends IntermediaryMinecraftProvider<SplitMinecraftProvider> implements Split {
		public SplitImpl(ConfigContext configContext, SplitMinecraftProvider minecraftProvider) {
			super(configContext, minecraftProvider);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
				new RemappedJars(minecraftProvider.getMinecraftCommonJar(), getCommonJar(), MappingsNamespace.OFFICIAL),
				new RemappedJars(minecraftProvider.getMinecraftClientOnlyJar(), getClientOnlyJar(), MappingsNamespace.OFFICIAL, minecraftProvider.getMinecraftCommonJar())
			);
		}

		@Override
		protected void configureRemapper(RemappedJars remappedJars, TinyRemapper.Builder tinyRemapperBuilder) {
			final MinecraftJar outputJar = remappedJars.outputJar();
			assert !outputJar.isMerged();

			if (outputJar.includesClient()) {
				assert !outputJar.includesServer();
				tinyRemapperBuilder.extraPostApplyVisitor(SidedClassVisitor.CLIENT);
			}
		}
	}

	public static final class SingleJarImpl extends IntermediaryMinecraftProvider<SingleJarMinecraftProvider> implements SingleJar {
		private final SingleJarEnvType env;

		private SingleJarImpl(ConfigContext configContext, SingleJarMinecraftProvider minecraftProvider, SingleJarEnvType env) {
			super(configContext, minecraftProvider);
			this.env = env;
		}

		public static SingleJarImpl server(ConfigContext configContext, SingleJarMinecraftProvider minecraftProvider) {
			return new SingleJarImpl(configContext, minecraftProvider, SingleJarEnvType.SERVER);
		}

		public static SingleJarImpl client(ConfigContext configContext, SingleJarMinecraftProvider minecraftProvider) {
			return new SingleJarImpl(configContext, minecraftProvider, SingleJarEnvType.CLIENT);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
				new RemappedJars(minecraftProvider.getMinecraftEnvOnlyJar(), getEnvOnlyJar(), MappingsNamespace.OFFICIAL)
			);
		}

		@Override
		public SingleJarEnvType env() {
			return env;
		}
	}

	public static final class GluedImpl extends IntermediaryMinecraftProvider<GluedMinecraftProvider> implements Merged {
		public GluedImpl(ConfigContext configContext, GluedMinecraftProvider minecraftProvider) {
			super(configContext, minecraftProvider);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
				new RemappedJars(minecraftProvider.getMergedJar(), getMergedJar(), MappingsNamespace.GLUE)
			);
		}
	}
}
