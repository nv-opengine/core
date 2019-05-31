package com.gracefulcode.opengine.v2.vulkan.plugins;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.v2.vulkan.ExtensionConfiguration;
import com.gracefulcode.opengine.v2.vulkan.LayerConfiguration;
import com.gracefulcode.opengine.v2.vulkan.PhysicalDevice;
import com.gracefulcode.opengine.v2.vulkan.plugins.interfaces.FiltersPhysicalDevices;
import com.gracefulcode.opengine.v2.vulkan.plugins.interfaces.NeedsQueues;
import com.gracefulcode.opengine.v2.vulkan.Vulkan;
import com.gracefulcode.opengine.v2.vulkan.Window;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

/**
 * Adds window manager capabilities to Opengine. Without this plugin, you
 * cannot open windows at all!
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class WindowManager implements Plugin, FiltersPhysicalDevices, NeedsQueues {
	public static class ColorSpace {
		protected int colorSpace;

		public ColorSpace(int colorSpace) {
			this.colorSpace = colorSpace;
		}

		public String toString() {
			switch (this.colorSpace) {
				case 0:
					return "VK_COLOR_SPACE_SRGB_NONLINEAR_KHR";
				default:
					return "Unknown (" + this.colorSpace + ")";
			}
		}
	}

	public static class PresentMode {
		protected int presentMode;

		public PresentMode(int presentMode) {
			this.presentMode = presentMode;
		}

		public int buffering() {
			switch (this.presentMode) {
				case 0:
					return 1;
				case 1:
					return 3;
				case 2:
					return 2;
				case 3:
					return 2;
				default:
					return 1;
			}
		}

		public boolean canTear() {
			switch (this.presentMode) {
				case 0:
				case 3:
					return true;
				case 1:
				case 2:
					return false;
				default:
					return true;
			}
		}

		public String toString() {
			switch (this.presentMode) {
				case 0:
					return "VK_PRESENT_MODE_IMMEDIATE_KHR";
				case 1:
					return "VK_PRESENT_MODE_MAILBOX_KHR";
				case 2:
					return "VK_PRESENT_MODE_FIFO_KHR";
				case 3:
					return "VK_PRESENT_MODE_FIFO_RELAXED_KHR";
				default:
					return "Unknown (" + this.presentMode + ")";
			}
		}
	}

	public static class Format {
		protected int format;

		public Format(int format) {
			this.format = format;
		}

		public boolean isSRGB() {
			switch(this.format) {
				case 44:
				case 97:
					return false;
				case 50:
					return true;
				default:
					return false;
			}
		}

		public int bitsPerChannel() {
			switch (this.format) {
				case 44:
				case 50:
					return 8;
				case 97:
					return 16;
				default:
					return -1;
			}
		}

		public int channels() {
			switch (this.format) {
				case 44:
				case 50:
				case 97:
					return 4;
				default:
					return -1;
			}
		}

		public String toString() {
			switch (this.format) {
				case 44:
					return "VK_FORMAT_B8G8R8A8_UNORM";
				case 50:
					return "VK_FORMAT_B8G8R8A8_SRGB";
				case 97:
					return "VK_FORMAT_R16G16B16A16_SFLOAT";
				default:
					return "Unknown (" + this.format + ")";
			}
		}
	}

	public static class DefaultPossibilityComparator implements Comparator<Possibility> {
		public int compare(Possibility a, Possibility b) {
			// Return the dedicated graphics card if one exists.
			int comparison = Integer.compare(b.physicalDevice.deviceType(), a.physicalDevice.deviceType());
			if (comparison != 0) return comparison;

			// If one supports a higher level of API, it's probably a better card?
			// Or at least the one with the most up to date graphics driver.
			comparison = Integer.compare(b.physicalDevice.apiVersion(), a.physicalDevice.apiVersion());
			if (comparison != 0) return comparison;

			if (b.presentMode.canTear() && !a.presentMode.canTear()) return -1;
			if (a.presentMode.canTear() && !b.presentMode.canTear()) return 1;

			comparison = Integer.compare(b.presentMode.buffering(), a.presentMode.buffering());
			if (comparison != 0) return comparison;

			if (b.format.isSRGB() && !a.format.isSRGB()) return -1;
			if (a.format.isSRGB() && !b.format.isSRGB()) return 1;

			comparison = Integer.compare(b.format.channels(), a.format.channels());
			if (comparison != 0) return comparison;

			comparison = Integer.compare(b.format.bitsPerChannel(), a.format.bitsPerChannel());
			if (comparison != 0) return comparison;

			if (a.graphicsQueue == a.presentQueue && b.graphicsQueue != b.presentQueue) return -1;
			if (b.graphicsQueue == b.presentQueue && a.graphicsQueue != a.presentQueue) return 1;

			// comparison = Integer.compare(b.hashCode(), a.hashCode());
			// if (comparison != 0) return comparison;

			return 0;
		}
	}

	public static class Configuration {
		public Comparator<Possibility> possibilityComparator = new DefaultPossibilityComparator();
	}
	/**
	 * A combination of setup parameters that is possible to use. Can be sorted
	 * and chosen by the end user.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1.1
	 * @since 0.1.1
	 */
	public static class Possibility {
		public PhysicalDevice physicalDevice;
		public ColorSpace colorSpace;
		public Format format;
		public PresentMode presentMode;

		/**
		 * TODO: Replace all of these with objects that lets the end user ask
		 * things like "is this a dedicated queue?" or "Does this color space
		 * support this many alpha bits?"
		 */
		public int graphicsQueue;
		public int presentQueue;
		public int computeQueue;

		public String toString() {
			return "\n\tPossibility[\n\t\tphysicalDevice: " + physicalDevice.toString() + ",\n\t\tpresentMode: " + this.presentMode + ",\n\t\tformat: " + this.format + ",\n\t\tcolorSpace: " + this.colorSpace + ",\n\t\tgraphics: " + this.graphicsQueue + ",\n\t\tpresent: " + this.presentQueue + ",\n\t\tcompute: " + this.computeQueue + "\n\t]";
		}
	}

	protected ArrayList<Window> openWindows = new ArrayList<Window>();
	protected VkInstance vkInstance;
	protected Vulkan vulkan;
	protected Configuration configuration;

	public WindowManager() {
		this(new Configuration());
	}

	public WindowManager(Configuration configuration) {
		if (!glfwInit()) {
			throw new AssertionError("GLFW Failed to initialize.");
		}
		
		this.configuration = configuration;
	}

	@Override
	public boolean graphicsPresentationMustMatch() {
		return true;
	}

	@Override
	public boolean computeGraphicsMustMatch() {
		return false;
	}

	@Override
	public int numComputeQueues() {
		return 0;
	}

	@Override
	public int numPresentationQueues() {
		return 1;
	}

	@Override
	public int numGraphicsQueues() {
		return 1;
	}

	@Override
	public void setupExtensions(ExtensionConfiguration configuration) {
		PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
		if (requiredExtensions == null) {
			throw new AssertionError("Failed to find list of required Vulkan extensions");
		}

		for (int i = 0; i < requiredExtensions.limit(); i++) {
			configuration.setExtension(requiredExtensions.getStringUTF8(i), ExtensionConfiguration.RequireType.REQUIRED);
		}
	}

	@Override
	public void setupLayers(LayerConfiguration configuration) {
	}

	@Override
	public boolean canUsePhysicalDevice(PhysicalDevice physicalDevice) {
		return true;
	}

	protected void getQueues(PhysicalDevice physicalDevice, long surface, ArrayList<Integer> possibleGraphics, ArrayList<Integer> possiblePresent, ArrayList<Integer> possibleCompute) {
		IntBuffer supportsPresent = memAllocInt(1);

		ArrayList<PhysicalDevice.Queue> queues = physicalDevice.getQueues();

		for (int i = 0; i < queues.size(); i++) {
			PhysicalDevice.Queue queue = queues.get(i);

			// No graphics? Pass!
			if ((queue.flags & VK_QUEUE_GRAPHICS_BIT) != 0) {
				possibleGraphics.add(i);
			}

			int err = vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice.getPhysicalDevice(), i, surface, supportsPresent);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to physical device surface support: " + Vulkan.translateVulkanResult(err));
			}

			if (supportsPresent.get(0) == VK_TRUE) {
				possiblePresent.add(i);
			}

			if ((queue.flags & VK_QUEUE_COMPUTE_BIT) != 0) {
				possibleCompute.add(i);
			}
		}
	}

	/**
	 * TODO: Make this configurable without subclassing.
	 */
	protected boolean isPossibilityAcceptable(Possibility p) {
		return true;
	}

	protected TreeSet<Possibility> chooseSetupForSurface(long surface) {
		TreeSet<Possibility> possibilities = new TreeSet<Possibility>(this.configuration.possibilityComparator);

		for (PhysicalDevice physicalDevice: this.vulkan.getPhysicalDevices()) {
			ArrayList<PhysicalDevice.Queue> queues = physicalDevice.getQueues();

			ArrayList<Integer> presentModes = new ArrayList<Integer>();
			ArrayList<Integer> formats = new ArrayList<Integer>();
			ArrayList<Integer> graphics = new ArrayList<Integer>();
			ArrayList<Integer> present = new ArrayList<Integer>();
			ArrayList<Integer> compute = new ArrayList<Integer>();

			this.getQueues(physicalDevice, surface, graphics, present, compute);

			VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc();
			int err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.getPhysicalDevice(), surface, surfaceCapabilities);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Unable to get device surface capabilities: " + Vulkan.translateVulkanResult(err));
			}

			System.out.println("imageCount: " + surfaceCapabilities.minImageCount() + "-" + surfaceCapabilities.maxImageCount());
			VkExtent2D extent = surfaceCapabilities.currentExtent();
			System.out.println("extent: " + extent.width() + "," + extent.height());
			System.out.println("max image array layers: " + surfaceCapabilities.maxImageArrayLayers());
			System.out.println("supported composite alpha: " + surfaceCapabilities.supportedCompositeAlpha());
			System.out.println("supported usage: " + surfaceCapabilities.supportedUsageFlags());

			surfaceCapabilities.free();

			IntBuffer pPresentModeCount = memAllocInt(1);
			err = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.getPhysicalDevice(), surface, pPresentModeCount, null);
			int presentModeCount = pPresentModeCount.get(0);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to get number of physical device surface presentation modes: " + Vulkan.translateVulkanResult(err));
			}

			IntBuffer pPresentModes = memAllocInt(presentModeCount);
			err = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.getPhysicalDevice(), surface, pPresentModeCount, pPresentModes);
			memFree(pPresentModeCount);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to get physical device surface presentation modes: " + Vulkan.translateVulkanResult(err));
			}

			for (int i = 0; i < presentModeCount; i++) {
				presentModes.add(pPresentModes.get(i));
			}

			IntBuffer pFormatCount = memAllocInt(1);
			err = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getPhysicalDevice(), surface, pFormatCount, null);
			int formatCount = pFormatCount.get(0);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to query number of physical device surface formats: " + Vulkan.translateVulkanResult(err));
			}

			VkSurfaceFormatKHR.Buffer surfFormats = VkSurfaceFormatKHR.calloc(formatCount);
			err = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getPhysicalDevice(), surface, pFormatCount, surfFormats);
			memFree(pFormatCount);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to query physical device surface formats: " + Vulkan.translateVulkanResult(err));
			}

			for (int i = 0; i < surfFormats.limit(); i++) {
				for (int m = 0; m < presentModes.size(); m++) {
					for (Integer graphicsQueue: graphics) {
						for (Integer presentQueue: present) {
							for (Integer computeQueue: compute) {
								Possibility possibility = new Possibility();
								possibility.physicalDevice = physicalDevice;
								possibility.presentMode = new PresentMode(presentModes.get(m));
								possibility.format = new Format(surfFormats.get(i).format());
								possibility.colorSpace = new ColorSpace(surfFormats.get(i).colorSpace());
								possibility.graphicsQueue = graphicsQueue;
								possibility.presentQueue = presentQueue;
								possibility.computeQueue = computeQueue;

								if (this.isPossibilityAcceptable(possibility)) {
									possibilities.add(possibility);
								}
							}
						}
					}
				}
			}
		}

		System.out.println(possibilities.toString());

		return possibilities;
	}

	public Window createWindow(Window.Configuration configuration) {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
		// glfwWindowHint(GLFW_RED_BITS, 8);
		// glfwWindowHint(GLFW_GREEN_BITS, 8);
		// glfwWindowHint(GLFW_BLUE_BITS, 8);
		// glfwWindowHint(GLFW_ALPHA_BITS, 8);
		glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE);

		long id = glfwCreateWindow(
			configuration.width,
			configuration.height,
			configuration.title,
			0,
			0
		);

		LongBuffer pSurface = memAllocLong(1);
		int err = glfwCreateWindowSurface(this.vkInstance, id, null, pSurface);
		final long surface = pSurface.get(0);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create surface: " + Vulkan.translateVulkanResult(err));
		}
		memFree(pSurface);

		this.chooseSetupForSurface(surface);

		// if (configuration.keyCallback != null) {
		// 	glfwSetKeyCallback(id, configuration.keyCallback);
		// }

		glfwShowWindow(id);

		Window vw = new Window(id);
		this.openWindows.add(vw);
		return vw;		
	}

	/**
	 * Ticks every open window. Should be called every frame.
	 *
	 * @return True if we should close the app, otherwise false.
	 */
	public boolean tick() {
		glfwPollEvents();

		// TODO: If our window was focused and is now being closed, we should focus another one of our windows.
		// TODO: We should maybe let the user control this since we're in engine code here.
		for (int i = this.openWindows.size() - 1; i >= 0; i--) {
			Window w = this.openWindows.get(i);
			if (w.shouldClose()) {
				this.openWindows.remove(i);
				w.close();
				w.dispose();
			}
		}

		if (this.openWindows.isEmpty()) return true;
		return false;
	}

	@Override
	public void setupCreateInfo(VkInstanceCreateInfo createInfo) {

	}

	@Override
	public void postCreate(Vulkan vulkan, VkInstance instance, ExtensionConfiguration extensionConfiguration, LayerConfiguration LayerConfiguration) {
		this.vkInstance = instance;
		this.vulkan = vulkan;
	}

	@Override
	public void dispose() {

	}
}