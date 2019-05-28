package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;

import java.util.ArrayList;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

/**
 * A physical device surface represents, essentially a single drawing
 * surface for this physical device. In practical terms, this means a
 * window.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class PhysicalDeviceSurface {
	/**
	 * The physical device that we're using here.
	 */
	protected VulkanPhysicalDevice device;

	/**
	 * The id for the surface in question.
	 */
	protected Surface surface;

	/**
	 * Capabilities of the surface irrespective of configuration options. We
	 * will get more capability-type things once we choose some more details,
	 * but this gives us a nice baseline.
	 *
	 * minImageCount / maxImageCount
	 *     The minimum and maximum amounts of images that you are allowed to
	 *     use in a swapchain. Opengine should take care of this for you.
	 *     GTX 1070: 2-8
	 * minImageExtent / currentExtent / maxImageExtent
	 *     Image extents are widths and heights. These define the smallest and
	 *     largest image you can have backing this surface, and the current
	 *     size. In MOST cases, these will all be the same -- the size of the
	 *     window itself. This will be used by Opengine when you resize the
	 *     window. You often won't have to use it yourself, but you can.
	 *     GTX 1070: all same
	 * maxImageArrayLayers
	 *     Video cards support so-called "3d" textures and images. This tells
	 *     you how many layers (the z-axis or depth) is supported on the actual
	 *     framebuffer. This will probably be 1 unless you're programming for
	 *     VR (some VR systems represent the different eyes as a depth-2
	 *     image).
	 *     GTX 1070: 1
	 * currentTransform / supportedTransforms
	 *     Transforms are automatic transformations that happen before hitting
	 *     the screen. This mostly has to do with mobile phones and being
	 *     flipped around. You don't have to handle this yourself -- you can
	 *     just ignore it. Vulkan simply tells you so that you CAN do
	 *     something weird.
	 *     All start with VK_SURFACE_TRANSFORM_
	 *         IDENTIY_BIT_KHR: The identity transform, ie none.
	 *         ROTATE_90_BIT_KHR: 90 degree rotation
	 *         ROTATE_180_BIT_KHR: 180 degree rotation
	 *         ROTATE_270_BIT_KHR: 270 degree rotation
	 *         HORIZONTAL_MIRROR_BIT_KHR: Mirrored horizontally, otherwise identity
	 *         HORIZONTAL_ROTATE_90_BIT_KHR: Mirrored hirozontally and rotated 90 degrees
	 *         HORIZONTAL_ROTATE_180_BIT_KHR: Mirrored hirozontally and rotated 180 degrees
	 *         HORIZONTAL_ROTATE_270_BIT_KHR: Mirrored hirozontally and rotated 270 degrees
	 *     GTX 1070: Only identity supported.
	 * supportedCompositeAlpha
	 *     Can you make a transparent window and how?
	 *         VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR: Opaque window, the norm.
	 *         VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR: Pre-multiplied alpha.
	 *         VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR: Post-multiplied alpha.
	 *         VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR: Inherit from native commands, Vulkan won't handle it.
	 *     GTX 1070: Only opaque!
	 * supportedUsageFlags
	 *     What can we use the framebuffer image for?
	 *         VK_IMAGE_USAGE_TRANSFER_SRC_BIT
	 *         VK_IMAGE_USAGE_TRANSFER_DST_BIT
	 *         VK_IMAGE_USAGE_SAMPLED_BIT
	 *         VK_IMAGE_USAGE_STORAGE_BIT
	 *         VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
	 *         VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
	 *         VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT
	 *         VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT
	 *     GTX 1070: 
	 *         VK_IMAGE_USAGE_TRANSFER_SRC_BIT
	 *         VK_IMAGE_USAGE_TRANSFER_DST_BIT
	 *         VK_IMAGE_USAGE_SAMPLED_BIT
	 *         VK_IMAGE_USAGE_STORAGE_BIT
	 *         VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
	 *         VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT
	 */
	protected VkSurfaceCapabilitiesKHR capabilities;

	protected ArrayList<Format> formats = new ArrayList<Format>();
	protected ArrayList<PresentMode> presentModes = new ArrayList<PresentMode>();

	protected IntBuffer ib;

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

	public PhysicalDeviceSurface(VulkanPhysicalDevice device, Surface surface) {
		this.device = device;
		this.surface = surface;

		this.capabilities = VkSurfaceCapabilitiesKHR.calloc();

		this.ib = memAllocInt(1);

		System.out.println("Here: " + this.device);
		vkGetPhysicalDeviceSurfaceFormatsKHR(this.device.device, this.surface.getId(), this.ib, null);

		VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(this.ib.get(0));
		vkGetPhysicalDeviceSurfaceFormatsKHR(this.device.device, this.surface.getId(), this.ib, formats);

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
		vkGetPhysicalDeviceSurfacePresentModesKHR(this.device.device, this.surface.getId(), this.ib, null);
		if (ib.get(0) != 0) {
			IntBuffer ib2 = memAllocInt(ib.get(0));
			vkGetPhysicalDeviceSurfacePresentModesKHR(this.device.device, this.surface.getId(), this.ib, ib2);
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

	public VkSurfaceCapabilitiesKHR getCapabilities() {
		return this.capabilities;
	}

	public Surface getSurface() {
		return this.surface;
	}

	public String toString() {
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(this.device.device, this.surface.getId(), this.capabilities);

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

		System.out.println("Formats:");
		for (Format f: this.formats) {
			System.out.println("    -");
			System.out.println("     Format: " + Vulkan.translateFormat(f.format));
			System.out.println("     Color Space: " + f.colorSpace);
		}

		System.out.println("Present Mode:");
		for (PresentMode pm: this.presentModes) {
			System.out.println("    " + Vulkan.translatePresentMode(pm.mode));
		}
		return "";
	}

	public void dispose() {
		this.capabilities.free();
	}
}
