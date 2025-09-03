package org.springframework.ai.mcp.sample.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * MCP 服务端配置
 */
@Configuration
public class McpServerConfig {

	// STDIO transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
	public StdioServerTransportProvider stdioServerTransportProvider() {
		// 标准IO 服务端传输通信提供者
		return new StdioServerTransportProvider();
	}

	// SSE transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public WebFluxSseServerTransportProvider sseServerTransportProvider() {
		// 流式SSE 服务端传输通信提供者
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
		return new WeatherApiClient();
	}

	@Bean
	public McpSyncServer mcpServer(McpServerTransportProvider transportProvider, WeatherApiClient weatherApiClient) { // @formatter:off

		// Configure server capabilities with resource support
		// 服务端功能
		var capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true) // Tool support with list changes notifications
			.logging() // Logging support
			.build();

		// Create the server with both tool and resource capabilities
		// MCP 同步服务端
		McpSyncServer server = McpServer.sync(transportProvider)
			.serverInfo("MCP Demo Weather Server", "1.0.0") // 服务端信息
			.capabilities(capabilities)
			.tools(McpToolUtils.toSyncToolSpecifications(ToolCallbacks.from(weatherApiClient))) // Add @Tools
			.build();
		
		return server; // @formatter:on
	} // @formatter:on

}
