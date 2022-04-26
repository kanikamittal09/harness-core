/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

@Slf4j
public class OpenApiGenerationCommand extends Command {
  public static final String OUTPUT_FILE_PATH = "outputFilePath";
  public static final String LEADING_AND_TRAILING_QUOTES = "^\"|\"$";
  public static final String NEW_LINE = "\\n";
  public static final String BACKWARD_SLASH = "\\";
  private final OpenAPIConfiguration oasConfig;
  private OutputStream outputStream;

  public OpenApiGenerationCommand(OpenAPIConfiguration oasConfig) {
    super("generate-openapi-spec", "Generates Openapi 3 specification file");
    this.oasConfig = oasConfig;
  }

  @VisibleForTesting
  public OpenApiGenerationCommand(OpenAPIConfiguration oasConfig, OutputStream outputStream) {
    this(oasConfig);
    this.outputStream = outputStream;
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument(OUTPUT_FILE_PATH).help("Absolute path to output openapi spec file");
  }

  @Override
  public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
    String outputFilePath = namespace.getString(OUTPUT_FILE_PATH);

    LocalOpenAPIResource localOpenAPIResource = new LocalOpenAPIResource();
    SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration()
                                                    .openAPI(oasConfig.getOpenAPI())
                                                    .prettyPrint(true)
                                                    .scannerClass(oasConfig.getScannerClass())
                                                    .resourceClasses(oasConfig.getResourceClasses());

    localOpenAPIResource.setOpenApiConfiguration(swaggerConfiguration);
    Response json = localOpenAPIResource.getOpenApi(null, null, null, null, APPLICATION_JSON_TYPE.getSubtype());

    try (OutputStream out = outputStream != null ? outputStream : new FileOutputStream(outputFilePath)) {
      String openApiSpecContent = sanitize(json);
      out.write(openApiSpecContent.getBytes());
    } catch (Exception exception) {
      log.error("Failed to generate OpenAPI spec at location : " + OUTPUT_FILE_PATH + " because of : " + exception);
    }
  }

  private String sanitize(Response json) throws JsonProcessingException {
    String openApiSpecContent = new ObjectMapper().writeValueAsString(json.getEntity());
    return openApiSpecContent.replace(NEW_LINE, EMPTY)
        .replace(BACKWARD_SLASH, EMPTY)
        .replaceAll(LEADING_AND_TRAILING_QUOTES, EMPTY);
  }

  static class LocalOpenAPIResource extends BaseOpenApiResource {
    @Override
    protected Response getOpenApi(
        HttpHeaders headers, ServletConfig config, Application app, UriInfo uriInfo, String type) throws Exception {
      return super.getOpenApi(headers, config, app, uriInfo, type);
    }
  }
}
