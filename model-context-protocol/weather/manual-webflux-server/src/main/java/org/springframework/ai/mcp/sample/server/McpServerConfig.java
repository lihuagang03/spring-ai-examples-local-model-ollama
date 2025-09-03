package org.springframework.ai.mcp.sample.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

import java.util.Arrays;
import java.util.List;

/**
 * MCP 服务器配置
 */
@Configuration
public class McpServerConfig {

	// STDIO transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
	public StdioServerTransportProvider stdioServerTransportProvider() {
		// 标准IO 服务器传输通信提供者
		return new StdioServerTransportProvider();
	}

	// SSE transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public WebFluxSseServerTransportProvider sseServerTransportProvider() {
		// 流式SSE 服务器传输通信提供者
		return new WebFluxSseServerTransportProvider(new ObjectMapper(), "/mcp/message");
	}

	// Router function for SSE transport used by Spring WebFlux to start an HTTP
	// server.
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransportProvider transportProvider) {
		// 路由函数，SSE传输
		return transportProvider.getRouterFunction();
	}

	@Bean
	public WeatherApiClient weatherApiClient() {
		// WeatherApiClient 工具对象实例
		return new WeatherApiClient();
	}

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Bean
	public McpSyncServer mcpServer(McpServerTransportProvider transportProvider, WeatherApiClient weatherApiClient) { // @formatter:off

		// Configure server capabilities with resource support
		// 服务器功能
		var capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true) // Tool support with list changes notifications
			.logging() // Logging support
			.build();

		// 工具列表
		ToolCallback[] toolCallbacks = ToolCallbacks.from(weatherApiClient);
		System.out.println("\ntoolCallbacks:");
		Arrays.stream(toolCallbacks).forEach(toolCallback -> {
			ToolDefinition toolDefinition = toolCallback.getToolDefinition();
			try {
				String jsonString = OBJECT_MAPPER.writeValueAsString(toolDefinition);
				System.out.println(jsonString);
				System.out.println();
			} catch (JsonProcessingException e) {
//				e.printStackTrace();
			}
		});

		List<McpServerFeatures.SyncToolSpecification> toolSpecifications =
				McpToolUtils.toSyncToolSpecifications(toolCallbacks);
		System.out.println("\ntoolSpecifications:");
		toolSpecifications.forEach(toolSpecification -> {
			McpSchema.Tool tool = toolSpecification.tool();
			try {
				String jsonString = OBJECT_MAPPER.writeValueAsString(tool);
				System.out.println(jsonString);
				System.out.println();
			} catch (JsonProcessingException e) {
//				e.printStackTrace();
			}
		});

		// Create the server with both tool and resource capabilities
		// MCP服务器
		McpSyncServer server = McpServer.sync(transportProvider)
				.serverInfo("MCP Demo Weather Server", "1.0.0") // 服务器信息
				.capabilities(capabilities)
				.tools(toolSpecifications) // Add @Tools
				.build();
		
		return server; // @formatter:on
	} // @formatter:on

}
