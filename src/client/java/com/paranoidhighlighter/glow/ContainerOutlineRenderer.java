package com.paranoidhighlighter.glow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class ContainerOutlineRenderer {
	private ContainerOutlineRenderer() {
	}

	public static void render(WorldRenderContext context) {
		var highlights = GlowTracker.blockHighlights();
		if (highlights.isEmpty()) {
			return;
		}
		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		PoseStack matrices = context.matrices();
		matrices.pushPose();
		matrices.translate(-camera.x, -camera.y, -camera.z);
		VertexConsumer buffer = context.consumers().getBuffer(RenderTypes.lines());
		PoseStack.Pose pose = matrices.last();
		for (GlowTracker.BlockHighlight highlight : highlights) {
			drawBox(buffer, pose, highlight.pos(), highlight.color());
		}
		matrices.popPose();
	}

	private static void drawBox(VertexConsumer buffer, PoseStack.Pose pose, BlockPos pos, int rgb) {
		int color = 0xFF000000 | (rgb & 0xFFFFFF);
		float x0 = pos.getX() - 0.002F;
		float y0 = pos.getY() - 0.002F;
		float z0 = pos.getZ() - 0.002F;
		float x1 = pos.getX() + 1.002F;
		float y1 = pos.getY() + 1.002F;
		float z1 = pos.getZ() + 1.002F;
		line(buffer, pose, x0, y0, z0, x1, y0, z0, color);
		line(buffer, pose, x1, y0, z0, x1, y0, z1, color);
		line(buffer, pose, x1, y0, z1, x0, y0, z1, color);
		line(buffer, pose, x0, y0, z1, x0, y0, z0, color);
		line(buffer, pose, x0, y1, z0, x1, y1, z0, color);
		line(buffer, pose, x1, y1, z0, x1, y1, z1, color);
		line(buffer, pose, x1, y1, z1, x0, y1, z1, color);
		line(buffer, pose, x0, y1, z1, x0, y1, z0, color);
		line(buffer, pose, x0, y0, z0, x0, y1, z0, color);
		line(buffer, pose, x1, y0, z0, x1, y1, z0, color);
		line(buffer, pose, x1, y0, z1, x1, y1, z1, color);
		line(buffer, pose, x0, y0, z1, x0, y1, z1, color);
	}

	private static void line(VertexConsumer buffer, PoseStack.Pose pose,
			float x1, float y1, float z1, float x2, float y2, float z2, int color) {
		float nx = x2 - x1;
		float ny = y2 - y1;
		float nz = z2 - z1;
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length > 0.0001F) {
			nx /= length;
			ny /= length;
			nz /= length;
		}
		buffer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, ny, nz);
		buffer.addVertex(pose, x2, y2, z2).setColor(color).setNormal(pose, nx, ny, nz);
	}
}
