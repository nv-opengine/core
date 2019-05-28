package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

/**
 * The main interface between end-user code and the Vulkan backend.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public class Vulkan {
	/**
	 * Configuration for Vulkan.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1.1
	 * @since 0.1
	 */
	public static class Configuration {
		/**
		 * Your application name is passed to Vulkan in case your game gets
		 * super popular and they want to tweak their driver for your game.
		 * This should be something unique, but it is not that important at
		 * the end of the day.
		 */
		public String applicationName = "";
	}

	/**
	 * The configuration that we are using.
	 */
	protected Configuration configuration;

	/**
	 * Initializing Vulkan multiple times is not allowed. This is the one and
	 * only instance.
	 */
	protected static Vulkan instance;

	/**
	 * Initialize the vulkan singleton.
	 *
	 * @param configuration The configuration of the resultant Vulkan instance.
	 * @return An initialized Vulkan instance.
	 * @throws AssertionError If Vulkan has previously been initialized.
	 * @throws AssertionError If GLFW is unable to be initialized.
	 */
	public static Vulkan initialize(Configuration configuration) {
		if (Vulkan.instance != null) {
			throw new AssertionError("Cannot initialize multiple instances of Vulkan.");
		}

		if (!glfwInit()) {
			throw new AssertionError("Failed to initialize GLFW");
		}

		Vulkan.instance = new Vulkan(configuration);
		return Vulkan.instance;
	}

	/**
	 * Initialize the vulkan singleton.
	 *
	 * @return An initialized Vulkan instance.
	 * @throws AssertionError If Vulkan has previously been initialized.
	 */
	public static Vulkan initialize() {
		return Vulkan.initialize(new Configuration());
	}

	/**
	 * Gets the initialized Vulkan instance. Returns null if not already
	 * initialized, won't initialize for you.
	 *
	 * @return The initialized Vulkan instance or null if one has not been initialized yet.
	 */
	public static Vulkan get() {
		return Vulkan.instance;
	}

	/**
	 * Initialize Vulkan with specific configuration.
	 */
	private Vulkan(Configuration configuration) {
		this.configuration = configuration;

		/**
		 * appInfo is basic information about the application itself. There
		 * isn't anything super important here, though we do let Vulkan know
		 * about both the engine and the particular game so that it can change
		 * its behavior if there's a particular popular engine/game.
		 */
		VkApplicationInfo appInfo = VkApplicationInfo.calloc();
		appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		appInfo.pApplicationName(memUTF8(this.configuration.applicationName));
		appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
		appInfo.pEngineName(memUTF8("Opengine"));
		appInfo.engineVersion(1);
		appInfo.apiVersion(VK_MAKE_VERSION(1, 0, 2));

		/**
		 * Create info is pretty basic right now.
		 */
		VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		createInfo.pApplicationInfo(appInfo);

		/**
		 * requiredExtensions are the extensions that glfw needs to properly
		 * work. If our vulkan doesn't support these extensions, we should exit
		 * pretty early -- we can't do anything anyway.
		 */
		PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
		if (requiredExtensions == null) {
			throw new AssertionError("Failed to find list of required Vulkan extensions");
		}

		/**
		 * Now let's see what extensions this version of vulkan actually
		 * supports!
		 */
		IntBuffer ib = memAllocInt(1);
		vkEnumerateInstanceExtensionProperties((ByteBuffer)null, ib, null);
		int result = ib.get(0);

		VkExtensionProperties.Buffer buffer = VkExtensionProperties.calloc(result);
		vkEnumerateInstanceExtensionProperties((ByteBuffer)null, ib, buffer);

		int instanceExtensionCount = buffer.limit();

		PointerBuffer ppEnabledExtensionNames = memAllocPointer(result);

		ppEnabledExtensionNames.put(requiredExtensions);
		HashSet<String> enabledExtensions = new HashSet<String>();

		for (int m = 0; m < instanceExtensionCount; m++) {
			buffer.position(m);
			System.out.println("Extension: " + buffer.extensionNameString());
		}

	}

	public void dispose() {

	}
}