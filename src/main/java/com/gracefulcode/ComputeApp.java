package com.gracefulcode;

import static org.lwjgl.glfw.GLFW.*;

import com.gracefulcode.opengine.vulkan.Vulkan;
import com.gracefulcode.opengine.vulkan.MemoryManager;
import com.gracefulcode.opengine.vulkan.Pipeline;
import com.gracefulcode.opengine.vulkan.Shader;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ComputeApp {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (!glfwInit()) {
			throw new RuntimeException("Failed to initialize GLFW");
		}

		Vulkan.Configuration vulkanConfiguration = new Vulkan.Configuration();
		vulkanConfiguration.applicationName = "GOTBK";
		vulkanConfiguration.needGraphics = false;
		vulkanConfiguration.needCompute = true;

		Vulkan vulkan = new Vulkan(vulkanConfiguration);
		MemoryManager.Buffer bufferA = vulkan.createComputeBuffer("Compute: A", 1023);
		MemoryManager.Buffer bufferB = vulkan.createComputeBuffer("Compute: B", 1023);
		vulkan.doneAllocating();
		
		// Shader shader = vulkan.createComputeShader("comp.spv");

		// TODO: I am going to want to swap these back and forth, using last
		// "frame"'s data as the scratchpad for this "frame." The current
		// createBinding setup makes that hard/impossible. Rethink?
		// Create Binding is actually done in two steps. In the first one we're defining what KIND of binding it is.
		// Binding to a specific buffer is done later. That's essentially what I want, I just need to expose that.
		// shader.createBinding(0, bufferA);
		// shader.createBinding(1, bufferB);
		// shader.doneBinding();

		// Pipeline computePipeline = vulkan.createComputePipeline(shader, "main");

		/**
		 * This is what's left, in C++. We're very close. Gotta design how the API works:
		 */
		/*
        Now we shall start recording commands into the newly allocated command buffer. 
	        VkCommandBufferBeginInfo beginInfo = {};
    	    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
        	beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT; // the buffer is only submitted and used once in this application.
        	VK_CHECK_RESULT(vkBeginCommandBuffer(commandBuffer, &beginInfo)); // start recording commands.

        We need to bind a pipeline, AND a descriptor set before we dispatch.
        The validation layer will NOT give warnings if you forget these, so be very careful not to forget them.
	        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline);
    	    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout, 0, 1, &descriptorSet, 0, NULL);

        Calling vkCmdDispatch basically starts the compute pipeline, and executes the compute shader.
        The number of workgroups is specified in the arguments.
        If you are already familiar with compute shaders from OpenGL, this should be nothing new to you.
	        vkCmdDispatch(commandBuffer, (uint32_t)ceil(WIDTH / float(WORKGROUP_SIZE)), (uint32_t)ceil(HEIGHT / float(WORKGROUP_SIZE)), 1);

    	    VK_CHECK_RESULT(vkEndCommandBuffer(commandBuffer)); // end recording commands.


        Now we shall finally submit the recorded command buffer to a queue.
	        VkSubmitInfo submitInfo = {};
    	    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
        	submitInfo.commandBufferCount = 1; // submit a single command buffer
        	submitInfo.pCommandBuffers = &commandBuffer; // the command buffer to submit.

        We create a fence.
	        VkFence fence;
    	    VkFenceCreateInfo fenceCreateInfo = {};
        	fenceCreateInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
        	fenceCreateInfo.flags = 0;
        	VK_CHECK_RESULT(vkCreateFence(device, &fenceCreateInfo, NULL, &fence));

        We submit the command buffer on the queue, at the same time giving a fence.
	        VK_CHECK_RESULT(vkQueueSubmit(queue, 1, &submitInfo, fence));

        The command will not have finished executing until the fence is signalled.
        So we wait here.
        We will directly after this read our buffer from the GPU,
        and we will not be sure that the command has finished executing unless we wait for the fence.
        Hence, we use a fence here.
	        VK_CHECK_RESULT(vkWaitForFences(device, 1, &fence, VK_TRUE, 100000000000));

    	    vkDestroyFence(device, fence, NULL);
    	*/

		// WindowManager windowManager = new WindowManager(wmConfiguration);
		// Window window = windowManager.createWindow();
		// Window window2 = windowManager.createWindow();

		// while (true) {
		// 	boolean shouldClose = windowManager.tick();
		// 	if (shouldClose) break;
		// }

		vulkan.dispose();
    }
}
