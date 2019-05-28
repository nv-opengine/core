package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.v2.PhysicalDevice;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.PointerBuffer;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

/**
 * A Physical Device represents a GPU in the system. Many things ultimately
 * come through here, though we should endeavor to pass as much as possible to
 * other classes to make things manageable.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class VulkanPhysicalDevice implements PhysicalDevice<VulkanLogicalDevice> {
	/**
	 * Queues that are available for this Physical Device.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1
	 * @since 0.1
	 */
	public static class QueueFamilyProperties {
		public int index;
		public VkExtent3D minImageTransferGranularity;
		public int queueCount;
		public int queueFlags;
		public int timestampValidBits;
	}

	protected VkPhysicalDeviceProperties properties;
	protected HashMap<Long, PhysicalDeviceSurface> surfaceProperties = new HashMap<Long, PhysicalDeviceSurface>();
	protected ArrayList<QueueFamilyProperties> queueFamilyProperties = new ArrayList<QueueFamilyProperties>();
	protected IntBuffer ib;
	protected PointerBuffer pb;
	protected VkPhysicalDevice device;
	protected int graphicsQueueIndex = -1;

	public VulkanPhysicalDevice(VkPhysicalDevice physicalDevice) {
		this.ib = memAllocInt(1);
		this.pb = memAllocPointer(1);

		this.device = physicalDevice;
		this.properties = VkPhysicalDeviceProperties.calloc();
		vkGetPhysicalDeviceProperties(physicalDevice, this.properties);

		vkGetPhysicalDeviceQueueFamilyProperties(this.device, this.ib, null);
		int queueCount = this.ib.get(0);

		VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
		vkGetPhysicalDeviceQueueFamilyProperties(this.device, this.ib, queueProps);

		int queueFamilyIndex;
		for (queueFamilyIndex = 0; queueFamilyIndex < queueCount; queueFamilyIndex++) {
			VkQueueFamilyProperties properties = queueProps.get(queueFamilyIndex);

			QueueFamilyProperties qfp = new QueueFamilyProperties();
			qfp.index = queueFamilyIndex;
			qfp.minImageTransferGranularity = properties.minImageTransferGranularity();
			qfp.queueCount = properties.queueCount();
			qfp.queueFlags = properties.queueFlags();
			qfp.timestampValidBits = properties.timestampValidBits();

			if ((this.graphicsQueueIndex == -1) && ((qfp.queueFlags & VK_QUEUE_GRAPHICS_BIT) > 0)) {
				this.graphicsQueueIndex = queueFamilyIndex;
			}
			this.queueFamilyProperties.add(qfp);
		}

		queueProps.free();
	}

	public boolean canDisplayToSurface(Surface surface) {
		for (QueueFamilyProperties qpf: this.queueFamilyProperties) {
			vkGetPhysicalDeviceSurfaceSupportKHR(
				this.device,
				qpf.index,
				surface.getId(),
				this.ib
			);
			if (this.ib.get(0) == VK_TRUE) {
				if (!this.surfaceProperties.containsKey(surface)) {
					PhysicalDeviceSurface pds = new PhysicalDeviceSurface(this, surface);
					this.surfaceProperties.put(
						surface.getId(),
						pds
					);
				}
				return true;
			}
		}
		return false;
	}

	public PhysicalDeviceSurface getSurface(long surface) {
		return this.surfaceProperties.get(surface);
	}

	public VkPhysicalDevice getDevice() {
		return this.device;
	}

	public VulkanLogicalDevice createLogicalDevice(
		String[] requiredExtensions2,
		boolean hasGraphicsQueue,
		boolean hasComputeQueue
	) {
		String[] requiredExtensions = {
			VK_KHR_SWAPCHAIN_EXTENSION_NAME
			// "VK_EXT_debug_utils"
		};

		PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.length);
		for (int i = 0; i < requiredExtensions.length; i++) {
			ppEnabledExtensionNames.put(memUTF8(requiredExtensions[i]));
		}
		ppEnabledExtensionNames.flip();

		FloatBuffer pQueuePriorities = memAllocFloat(1).put(0.0f);
		pQueuePriorities.flip();

		VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1);
		queueCreateInfo.position(0);
		queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
		queueCreateInfo.queueFamilyIndex(this.graphicsQueueIndex);
		queueCreateInfo.pQueuePriorities(pQueuePriorities);

		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			.pNext(NULL)
			.pQueueCreateInfos(queueCreateInfo)
			.ppEnabledExtensionNames(ppEnabledExtensionNames)
			.ppEnabledLayerNames(null);

		int err = vkCreateDevice(this.device, deviceCreateInfo, null, this.pb);
		long device = this.pb.get(0);
		// ppEnabledExtensionNames.free();

		return new VulkanLogicalDevice(
			this,
			new VkDevice(device, this.device, deviceCreateInfo)
		);
	}

	/**
	 * TODO: This should be our own thing, not the VK_* constants.
	 */
	public int deviceType() {
		return this.properties.deviceType();
	}

	public String deviceTypeName() {
		switch (this.properties.deviceType()) {
			case VK_PHYSICAL_DEVICE_TYPE_OTHER:
				return "Other";
			case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU:
				return "Integrated GPU";
			case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU:
				return "Discrete GPU";
			case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU:
				return "Virtual GPU";
			case VK_PHYSICAL_DEVICE_TYPE_CPU:
				return "CPU";
		}
		return "Unknown";
	}

	/**
	 * TODO: This should be our own thing, not the VK_* constants.
	 */
	public int apiVersion() {
		return this.properties.apiVersion();
	}

	public String name() {
		return this.properties.deviceNameString();
	}

	public String toString() {
		String ret = "PhysicalDevice<";
		ret += "deviceType:" + this.deviceTypeName();
		ret += ",apiVersion:" + this.apiVersion();
		ret += ",name:" + this.name();
		ret += ",limits:<\n";

		VkPhysicalDeviceLimits limits = this.properties.limits();

		ret += "\tbufferImageGranularity:";
		ret += limits.bufferImageGranularity();

		ret += ",\n\tcompute:[";
			ret += "\n\t\tmaxSharedMemorySize:";
			ret += limits.maxComputeSharedMemorySize();
			ret += ",\n\t\tworkGroup:[";
			ret += "\n\t\t\tmaxCount:[";
			ret += limits.maxComputeWorkGroupCount(0);
			ret += ",";
			ret += limits.maxComputeWorkGroupCount(1);
			ret += ",";
			ret += limits.maxComputeWorkGroupCount(2);
			ret += "],\n\t\t\tmaxInvocations:";
			ret += limits.maxComputeWorkGroupInvocations();
			ret += ",\n\t\t\tmaxSize:[";
			ret += limits.maxComputeWorkGroupSize(0);
			ret += ",";
			ret += limits.maxComputeWorkGroupSize(1);
			ret += ",";
			ret += limits.maxComputeWorkGroupSize(2);
		ret += "]\n\t\t]\n\t],";

		ret += "\n\tdiscreteQueuePriorities:";
		ret += limits.discreteQueuePriorities();

		ret += "\n\tfragmentShader:[";
			ret += "\n\t\tmaxCombinedOutputResources:";
			ret += limits.maxFragmentCombinedOutputResources();
			ret += ",\n\t\tmaxDualSrcAttachments:";
			ret += limits.maxFragmentDualSrcAttachments();
			ret += ",\n\t\tmaxInputComponents:";
			ret += limits.maxFragmentInputComponents();
			ret += ",\n\t\tmaxOutputAttachments:";
			ret += limits.maxFragmentOutputAttachments();
		ret += "\n\t],";

		ret += "\n\tframeBuffer:[";

			ret += "\n\t\tcolorSampleCounts:";
			ret += limits.framebufferColorSampleCounts();
			ret += ",\n\t\tdepthSampleCounts:";
			ret += limits.framebufferDepthSampleCounts();
			ret += ",\n\t\tmaxNoAttachmentsSampleCounts:";
			ret += limits.framebufferNoAttachmentsSampleCounts();
			ret += ",\n\t\tmaxStencilSampleCounts:";
			ret += limits.framebufferStencilSampleCounts();
			ret += ",\n\t\tmaxHeight:";
			ret += limits.maxFramebufferHeight();
			ret += ",\n\t\tmaxLayers:";
			ret += limits.maxFramebufferLayers();
			ret += ",\n\t\tmaxWidth:";
			ret += limits.maxFramebufferWidth();

		ret += "\n\t],";

		ret += "\n\tgeometryShader:[";
			ret += "\n\t\tmaxInputComponents:";
			ret += limits.maxGeometryInputComponents();
			ret += ",\n\t\tmaxOutputComponents:";
			ret += limits.maxGeometryOutputComponents();
			ret += ",\n\t\tmaxOutputVertices:";
			ret += limits.maxGeometryOutputVertices();
			ret += ",\n\t\tmaxShaderInvocations:";
			ret += limits.maxGeometryShaderInvocations();
			ret += ",\n\t\tmaxTotalOutputComponents:";
			ret += limits.maxGeometryTotalOutputComponents();
		ret += "\n\t],";

		ret += "\n\tlineWidth:[";
			ret += "\n\t\tgranularity:";
			ret += limits.lineWidthGranularity();
			ret += ",\n\t\trange:[";
			ret += limits.lineWidthRange(0);
			ret += ",";
			ret += limits.lineWidthRange(1);
		ret += "]\n\t],";

		ret += "\n\tmaxClipDistances:";
		ret += limits.maxClipDistances();
		ret += ",\n\tmaxColorAttachments:";
		ret += limits.maxColorAttachments();
		ret += ",\n\tmaxCombinedClipAndCullDistances:";
		ret += limits.maxCombinedClipAndCullDistances();
		ret += ",\n\tmaxCullDistances:";
		ret += limits.maxCullDistances();

		ret += ",\n\tdescriptorSet:[";
			ret += "\n\t\tmaxBound:";
			ret += limits.maxBoundDescriptorSets();
			ret += ",\n\t\tmaxInputAttachments:";
			ret += limits.maxDescriptorSetInputAttachments();
			ret += ",\n\t\tmaxSampledImages:";
			ret += limits.maxDescriptorSetSampledImages();
			ret += ",\n\t\tmaxSamplers:";
			ret += limits.maxDescriptorSetSamplers();
			ret += ",\n\t\tmaxStorageBuffers:";
			ret += limits.maxDescriptorSetStorageBuffers();
			ret += ",\n\t\tmaxStorageBuffersDynamic:";
			ret += limits.maxDescriptorSetStorageBuffersDynamic();
			ret += ",\n\t\tmaxStorageImages:";
			ret += limits.maxDescriptorSetStorageImages();
			ret += ",\n\t\tmaxUniformBuffers:";
			ret += limits.maxDescriptorSetUniformBuffers();
			ret += ",\n\t\tmaxUniformBuffersDynamic:";
			ret += limits.maxDescriptorSetUniformBuffersDynamic();
		ret += "\n\t],";

		ret += "\n\tvertexShader:[";
			ret += "\n\t\tmaxInputAttributeOffset:";
			ret += limits.maxVertexInputAttributeOffset();
			ret += ",\n\t\tmaxInputAttributes:";
			ret += limits.maxVertexInputAttributes();
			ret += ",\n\t\tmaxInputBindings:";
			ret += limits.maxVertexInputBindings();
			ret += ",\n\t\tmaxOutputComponents:";
			ret += limits.maxVertexOutputComponents();
		ret += "\n\t],";

		ret += "\n\tviewport:[";
			ret += "\n\t\tboundsRange:[";
			ret += limits.viewportBoundsRange(0); 
			ret += ",";
			ret += limits.viewportBoundsRange(1); 
			ret += "],\n\t\tmax:";
			ret += limits.maxViewports();
			ret += ",\n\t\tmaxDimensions:";
			ret += limits.maxViewportDimensions(0);
			ret += "x";
			ret += limits.maxViewportDimensions(1);
			ret += ",\n\t\tsubPixelBits:";
			ret += limits.viewportSubPixelBits();
		ret += "\n\t],";

		ret += "\n\tmaxDrawIndexedIndexValue:";
		ret += limits.maxDrawIndexedIndexValue();
		ret += ",\n\tmaxDrawIndirectCount:";
		ret += limits.maxDrawIndirectCount();
		ret += ",\n\tmaxImageArrayLayers:";
		ret += limits.maxImageArrayLayers();
		ret += ",\n\tmaxImageDimension1D:";
		ret += limits.maxImageDimension1D();
		ret += ",\n\tmaxImageDimension2D:";
		ret += limits.maxImageDimension2D();
		ret += ",\n\tmaxImageDimension3D:";
		ret += limits.maxImageDimension3D();
		ret += ",\n\tmaxImageDimensionCube:";
		ret += limits.maxImageDimensionCube();
		ret += ",\n\tmaxInterpolationOffset:";
		ret += limits.maxInterpolationOffset();
		ret += ",\n\tmaxMemoryAllocationCount:";
		ret += limits.maxMemoryAllocationCount();
		ret += ",\n\tmaxPerStageDescriptorInputAttachments:";
		ret += limits.maxPerStageDescriptorInputAttachments();
		ret += ",\n\tmaxPerStageDescriptorSampledImages:";
		ret += limits.maxPerStageDescriptorSampledImages();
		ret += ",\n\tmaxPerStageDescriptorSamplers:";
		ret += limits.maxPerStageDescriptorSamplers();
		ret += ",\n\tmaxPerStageDescriptorStorageBuffers:";
		ret += limits.maxPerStageDescriptorStorageBuffers();
		ret += ",\n\tmaxPerStageDescriptorStorageImages:";
		ret += limits.maxPerStageDescriptorStorageImages();
		ret += ",\n\tmaxPerStageDescriptorUniformBuffers:";
		ret += limits.maxPerStageDescriptorUniformBuffers();
		ret += ",\n\tmaxPerStageResources:";
		ret += limits.maxPerStageResources();
		ret += ",\n\tmaxPushConstantsSize:";
		ret += limits.maxPushConstantsSize();
		ret += ",\n\tmaxSampleMaskWords:";
		ret += limits.maxSampleMaskWords();
		ret += ",\n\tmaxSamplerAllocationCount:";
		ret += limits.maxSamplerAllocationCount();
		ret += ",\n\tmaxSamplerAnisotropy:";
		ret += limits.maxSamplerAnisotropy();
		ret += ",\n\tmaxSamplerLodBias:";
		ret += limits.maxSamplerLodBias();
		ret += ",\n\tmaxStorageBufferRange:";
		ret += limits.maxStorageBufferRange();
		ret += ",\n\tmaxTessellationControlPerPatchOutputComponents:";
		ret += limits.maxTessellationControlPerPatchOutputComponents();
		ret += ",\n\tmaxTessellationControlPerVertexInputComponents:";
		ret += limits.maxTessellationControlPerVertexInputComponents();
		ret += ",\n\tmaxTessellationControlPerVertexOutputComponents:";
		ret += limits.maxTessellationControlPerVertexOutputComponents();
		ret += ",\n\tmaxTessellationControlTotalOutputComponents:";
		ret += limits.maxTessellationControlTotalOutputComponents();
		ret += ",\n\tmaxTessellationEvaluationInputComponents:";
		ret += limits.maxTessellationEvaluationInputComponents();
		ret += ",\n\tmaxTessellationEvaluationOutputComponents:";
		ret += limits.maxTessellationEvaluationOutputComponents();
		ret += ",\n\tmaxTessellationGenerationLevel:";
		ret += limits.maxTessellationGenerationLevel();
		ret += ",\n\tmaxTessellationPatchSize:";
		ret += limits.maxTessellationPatchSize();
		ret += ",\n\tmaxTexelBufferElements:";
		ret += limits.maxTexelBufferElements();
		ret += ",\n\tmaxTexelGatherOffset:";
		ret += limits.maxTexelGatherOffset();
		ret += ",\n\tmaxTexelOffset:";
		ret += limits.maxTexelOffset();
		ret += ",\n\tmaxUniformBufferRange:";
		ret += limits.maxUniformBufferRange();
		ret += ",\n\tminInterpolationOffset:";
		ret += limits.minInterpolationOffset();
		ret += ",\n\tminMemoryMapAlignment:";
		ret += limits.minMemoryMapAlignment();
		ret += ",\n\tminStorageBufferOffsetAlignment:";
		ret += limits.minStorageBufferOffsetAlignment();
		ret += ",\n\tminTexelBufferOffsetAlignment:";
		ret += limits.minTexelBufferOffsetAlignment();
		ret += ",\n\tminTexelGatherOffset:";
		ret += limits.minTexelGatherOffset();
		ret += ",\n\tminTexelOffset:";
		ret += limits.minTexelOffset();
		ret += ",\n\tminUniformBufferOffsetAlignment:";
		ret += limits.minUniformBufferOffsetAlignment();
		ret += ",\n\tmipmapPrecisionBits:";
		ret += limits.mipmapPrecisionBits();
		ret += ",\n\tnonCoherentAtomSize:";
		ret += limits.nonCoherentAtomSize();
		ret += ",\n\toptimalBufferCopyOffsetAlignment:";
		ret += limits.optimalBufferCopyOffsetAlignment();
		ret += ",\n\toptimalBufferCopyRowPitchAlignment:";
		ret += limits.optimalBufferCopyRowPitchAlignment();
		ret += ",\n\tpointSizeGranularity:";
		ret += limits.pointSizeGranularity();
		ret += ",\n\tpointSizeRange:[";
		ret += limits.pointSizeRange(0);
		ret += ",";
		ret += limits.pointSizeRange(1);
		ret += "],\n\tsampledImageColorSampleCounts:";
		ret += limits.sampledImageColorSampleCounts();
		ret += ",\n\tsampledImageDepthSampleCounts:";
		ret += limits.sampledImageDepthSampleCounts();
		ret += ",\n\tsampledImageStencilSampleCounts:";
		ret += limits.sampledImageStencilSampleCounts();
		ret += ",\n\tsparseAddressSpaceSize:";
		ret += limits.sparseAddressSpaceSize();
		ret += ",\n\tstandardSampleLocations:";
		ret += limits.standardSampleLocations();
		ret += ",\n\tstorageImageSampleCounts:";
		ret += limits.storageImageSampleCounts();
		ret += ",\n\tstrictLines:";
		ret += limits.strictLines();
		ret += ",\n\tsubPixelInterpolationOffsetBits:";
		ret += limits.subPixelInterpolationOffsetBits();
		ret += ",\n\tsubPixelPrecisionBits:";
		ret += limits.subPixelPrecisionBits();
		ret += ",\n\tsubTexelPrecisionBits:";
		ret += limits.subTexelPrecisionBits();
		ret += ",\n\ttimestampComputeAndGraphics:";
		ret += limits.timestampComputeAndGraphics();
		ret += ",\n\ttimestampPeriod:";
		ret += limits.timestampPeriod();
		ret += ">";
		ret += ">";
		return ret;
	}

	public void dispose() {
		memFree(this.ib);
		this.properties.free();
		for (PhysicalDeviceSurface surface: this.surfaceProperties.values()) {
			surface.dispose();
		}
	}
}