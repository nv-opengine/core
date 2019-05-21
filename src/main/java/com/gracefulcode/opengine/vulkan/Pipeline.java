package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

public class Pipeline {
	public static class Configuration {
	}

	protected VulkanLogicalDevice logicalDevice;

	public Pipeline(SwapChain swapChain, VulkanLogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;

		VkPipelineVertexInputStateCreateInfo vertexInputCreate = VkPipelineVertexInputStateCreateInfo.calloc();
		vertexInputCreate.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
		vertexInputCreate.pVertexBindingDescriptions(null);
		vertexInputCreate.pVertexAttributeDescriptions(null);

		VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreate = VkPipelineInputAssemblyStateCreateInfo.calloc();
		inputAssemblyStateCreate.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
		inputAssemblyStateCreate.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
		inputAssemblyStateCreate.primitiveRestartEnable(false);

		VkViewport.Buffer viewport = VkViewport.calloc(1);
		viewport.x(0.0f);
		viewport.y(0.0f);
		VkExtent2D extent = swapChain.getExtent();
		viewport.width(extent.width());
		viewport.height(extent.height());
		viewport.minDepth(0.0f);
		viewport.maxDepth(1.0f);

		VkRect2D rect = VkRect2D.calloc();
		rect.offset().x(0).y(0);
		rect.extent(extent);

		VkRect2D.Buffer scissor = VkRect2D.calloc(1);
		scissor.offset().x(0).y(0);
		scissor.extent(extent);

		VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc();
		viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
		viewportState.pViewports(viewport);
		viewportState.pScissors(scissor);

		VkPipelineRasterizationStateCreateInfo rasterizationStateCreate = VkPipelineRasterizationStateCreateInfo.calloc();
		rasterizationStateCreate.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
		rasterizationStateCreate.depthClampEnable(false);
		rasterizationStateCreate.rasterizerDiscardEnable(false);
		rasterizationStateCreate.polygonMode(VK_POLYGON_MODE_FILL);
		rasterizationStateCreate.lineWidth(1.0f);
		rasterizationStateCreate.cullMode(VK_CULL_MODE_BACK_BIT);
		rasterizationStateCreate.frontFace(VK_FRONT_FACE_CLOCKWISE);
		rasterizationStateCreate.depthBiasEnable(false);
		rasterizationStateCreate.depthBiasConstantFactor(0.0f);
		rasterizationStateCreate.depthBiasClamp(0.0f);
		rasterizationStateCreate.depthBiasSlopeFactor(0.0f);

		VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc();
		multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
		multisampling.sampleShadingEnable(false);
		multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
		multisampling.minSampleShading(1.0f);
		multisampling.pSampleMask(null);
		multisampling.alphaToCoverageEnable(false);
		multisampling.alphaToOneEnable(false);

		VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1);
		colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
		colorBlendAttachment.blendEnable(false);
		colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_ONE);
		colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ZERO);
		colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
		colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
		colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
		colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);

		VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc();
		colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
		colorBlending.logicOpEnable(false);
		colorBlending.logicOp(VK_LOGIC_OP_COPY);
		colorBlending.pAttachments(colorBlendAttachment);
		colorBlending.blendConstants(0, 0.0f).blendConstants(1, 0.0f).blendConstants(2, 0.0f).blendConstants(3, 0.0f);

		VkPipelineLayoutCreateInfo pipelineLayoutCreate = VkPipelineLayoutCreateInfo.calloc();
		pipelineLayoutCreate.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
		pipelineLayoutCreate.pSetLayouts(null);
		pipelineLayoutCreate.pPushConstantRanges(null);

		LongBuffer lb = memAllocLong(1);
		int err = vkCreatePipelineLayout(this.logicalDevice.getDevice(), pipelineLayoutCreate, null, lb);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Error creating pipeline: " + Vulkan.translateVulkanResult(err));
		}

	}
}