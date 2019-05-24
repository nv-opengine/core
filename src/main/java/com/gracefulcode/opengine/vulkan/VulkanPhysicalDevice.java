package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.LogicalDevice;
import com.gracefulcode.opengine.PhysicalDevice;

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

public class VulkanPhysicalDevice implements PhysicalDevice<VulkanLogicalDevice> {
	public static class PhysicalDeviceSurface {
		protected VulkanPhysicalDevice device;
		protected long surface;
		protected VkSurfaceCapabilitiesKHR capabilities;
		protected IntBuffer ib;
		protected ArrayList<Format> formats = new ArrayList<Format>();
		protected ArrayList<PresentMode> presentModes = new ArrayList<PresentMode>();

		/**
		 * TODO: This should be our own thing, not the VK_* constants.
		 */
		public static class Format {
			public int format;
			public int colorSpace;
		}

		/**
		 * TODO: This should be our own thing, not the VK_* constants.
		 */
		public static class PresentMode {
			public int mode;
		}

		public PhysicalDeviceSurface(VulkanPhysicalDevice device, long surface) {
			this.device = device;
			this.surface = surface;

			this.capabilities = VkSurfaceCapabilitiesKHR.calloc();

			this.ib = memAllocInt(1);

			// Swapchain is being created before this?
			vkGetPhysicalDeviceSurfaceFormatsKHR(this.device.device, this.surface, this.ib, null);

			VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(this.ib.get(0));
			vkGetPhysicalDeviceSurfaceFormatsKHR(this.device.device, this.surface, this.ib, formats);

			for (int i = 0; i < formats.limit(); i++) {
				formats.position(i);

				Format format = new Format();
				format.format = formats.format();
				format.colorSpace = formats.colorSpace();
				this.formats.add(format);

				// COLOR SPACE:
				// 0: Mac
				// 0: Windows
				// Only defined value?!
				// VK_COLOR_SPACE_SRGB_NONLINEAR_KHR

				// FORMAT
				// Mac is 44, 50, 97
				// Windows is 2, 44, 50
				// Windows other is 4, 44, 50, 37, 43
				// VK_FORMAT_B8G8R8A8_UNORM <-- preferred
				// VK_FORMAT_B8G8R8A8_SRGB
				// VK_FORMAT_R16G16B16A16_SFLOAT
				// System.out.println("    Format: " + formats.format() + ", Color Space: " + formats.colorSpace());
			}
			formats.free();

			this.ib.clear();
			vkGetPhysicalDeviceSurfacePresentModesKHR(this.device.device, this.surface, this.ib, null);
			if (ib.get(0) != 0) {
				IntBuffer ib2 = memAllocInt(ib.get(0));
				vkGetPhysicalDeviceSurfacePresentModesKHR(this.device.device, this.surface, this.ib, ib2);
				for (int i = 0; i < ib2.limit(); i++) {
					PresentMode presentMode = new PresentMode();
					presentMode.mode = ib2.get(i);
					this.presentModes.add(presentMode);
					// Mac has 2, 0
					// Windows 1 has 2, 3, 1
					// Windows 2 has 0, 2
					// 0: PRESENT_MODE_IMMEDIATE_KHR <-- second if MAILBOX unavailable because FIFO can be buggy
					// 1: PRESENT_MODE_MAILBOX_KHR <-- preferred
					// 2: PRESENT_MODE_FIFO_KHR <-- guaranteed to exist, so third choice
					// 3: PRESENT_MODE_FIFO_RELAXED_KHR
					// Bad, might not even be in here
					// PRESENT_MODE_SHARED_DEMAND_REFRESH_KHR
					// PRESENT_MODE_SHARED_CONTINUOUS_REFRESH_KHR
				}
			}
		}

		public long getSurface() {
			return this.surface;
		}

		public String toString() {
			vkGetPhysicalDeviceSurfaceCapabilitiesKHR(this.device.device, this.surface, this.capabilities);

			System.out.println("Surface Capabilities:");
			// On mac this seems to always be the full size of the screen. I would expect at least one of these
			// extents to be the window size...
			// vulkan-tutorial.com says that it should be the window size...
			System.out.println("    Extent:");
			System.out.println("        Current: " + capabilities.currentExtent().width() + "x" + capabilities.currentExtent().height());
			System.out.println("        Max: " + capabilities.maxImageExtent().width() + "x" + capabilities.maxImageExtent().height());
			System.out.println("        Min: " + capabilities.minImageExtent().width() + "x" + capabilities.minImageExtent().height());
			// 000000001: Mac
			// 000000001: Windows
			// 000000001: IDENTITY
			// 000000010: ROTATE_90
			// 000000100: ROTATE_180
			// 000001000: ROTATE_270
			// 000010000: HORIZONTAL_MIRROR
			// 000100000: HORIZONTAL_MIRROR_ROTATE_90
			// 001000000: HORIZONTAL_MIRROR_ROTATE_180
			// 010000000: HORIZONTAL_MIRROR_ROTATE_270
			// 100000000: INHERIT
			System.out.println("    Transforms:");
			System.out.println("        Current: " + Integer.toBinaryString(capabilities.currentTransform()));
			System.out.println("        Supported: " + Integer.toBinaryString(capabilities.supportedTransforms()));

			System.out.println("    Max Image Array Layers: " + capabilities.maxImageArrayLayers());
			System.out.println("    Image Count: " + capabilities.minImageCount() + "-" + capabilities.maxImageCount());
			// 0111: Mac
			// 0001: Windows
			// 1001: Other Windows
			// 0001: ALPHA_OPAQUE_BIT
			// 0010: PRE_MULTIPLIED_BIT
			// 0100: POST_MULTIPLIED_BIT
			// 1000: INHERIT_BIT
			System.out.println("    Supported Composite Alpha: " + Integer.toBinaryString(capabilities.supportedCompositeAlpha()));
			System.out.println("    Supported Transforms: " + Integer.toBinaryString(capabilities.supportedTransforms()));
			// 00011111: Mac
			// 10011111: Windows
			// 00011111: Other Windows
			// 00000001: TRANSFER_SRC
			// 00000010: TRANSFER_DST
			// 00000100: SAMPLED
			// 00001000: STORAGE
			// 00010000: COLOR_ATTACHMENT
			// 00100000: DEPTH_STENCIL_ATTACHMENT
			// 01000000: TRANSIENT_ATTACHMENT
			// 10000000: INPUT_ATTACHMENT
			System.out.println("    Supported Usage Flags: " + Integer.toBinaryString(capabilities.supportedUsageFlags()));
			return "";
		}

		public void dispose() {
			this.capabilities.free();
		}
	}

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
	VkPhysicalDevice device;
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

	public boolean canDisplayToSurface(long surface) {
		for (QueueFamilyProperties qpf: this.queueFamilyProperties) {
			vkGetPhysicalDeviceSurfaceSupportKHR(
				this.device,
				qpf.index,
				surface,
				this.ib
			);
			if (this.ib.get(0) == VK_TRUE) {
				if (!this.surfaceProperties.containsKey(surface)) {
					this.surfaceProperties.put(
						surface,
						new PhysicalDeviceSurface(this, surface)
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
		String[] requiredExtensions,
		boolean hasGraphicsQueue,
		boolean hasComputeQueue
	) {
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