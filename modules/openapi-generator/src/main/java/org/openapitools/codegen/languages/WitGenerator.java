package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OpenAPI Generator for WebAssembly Interface Type (WIT) definitions.
 */
public final class WitGenerator extends DefaultCodegen implements CodegenConfig {
    
    // Remove unused logger

    private static final String PROJECT_NAME = "projectName";
    private static final String PACKAGE_NAME = "packageName";
    private static final String ERROR_MODEL = "errorModel";
    
    private String packageName = "openapi";

    public WitGenerator() {
        super();
        
        // Basic configuration
        templateDir = "wit";
        outputFolder = "generated-code/wit";
        
        // Reserved words in WIT
        reservedWords = new HashSet<>(Arrays.asList(
            "error", "expected", "list", "option", "result", "record",
            "variant", "enum", "flags", "type", "resource", "func",
            "static", "interface", "tuple", "u8", "u16", "u32", "u64",
            "s8", "s16", "s32", "s64", "float32", "float64", "bool",
            "string", "world", "export", "import", "package", "use"
        ));
        
        // Primitive types
        languageSpecificPrimitives = new HashSet<>(Arrays.asList(
            "string", "s32", "s64", "float32", "float64", "bool",
            "u8", "u16", "u32", "u64"
        ));
        
        // Type mappings
        typeMapping = new HashMap<>();
        typeMapping.put("string", "string");
        typeMapping.put("integer", "s32");
        typeMapping.put("long", "s64");
        typeMapping.put("float", "float32");
        typeMapping.put("double", "float64");
        typeMapping.put("boolean", "bool");
        typeMapping.put("array", "list");
        typeMapping.put("map", "record");
        typeMapping.put("date", "string");
        typeMapping.put("date-time", "string");
        typeMapping.put("binary", "list<u8>");
        typeMapping.put("file", "list<u8>");
        typeMapping.put("UUID", "string");
        typeMapping.put("URI", "string");
        typeMapping.put("object", "record");
        typeMapping.put("null", "option<string>");
        typeMapping.put("any", "string");
        
        // CLI options
        cliOptions.add(new CliOption(PROJECT_NAME, "Project name in generated WIT"));
        cliOptions.add(new CliOption(PACKAGE_NAME, "Package name for WIT namespace"));
        cliOptions.add(new CliOption(ERROR_MODEL, "Error model type (string, record, or variant)"));
        
        // Supporting files
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
    }

    @Override
    public void processOpts() {
        super.processOpts();
        
        if (additionalProperties.containsKey(PACKAGE_NAME)) {
            this.packageName = (String) additionalProperties.get(PACKAGE_NAME);
        }
        additionalProperties.put(PACKAGE_NAME, packageName);
        
        // Set up error model
        String errorModelCode = generateErrorModelCode();
        additionalProperties.put("errorType", errorModelCode);
    }

    private String generateErrorModelCode() {
        return "variant error {\n" +
               "    validation-error(record {\n" +
               "        message: string,\n" +
               "        details: list<record {\n" +
               "            field: string,\n" +
               "            message: string,\n" +
               "        }>,\n" +
               "    }),\n" +
               "    unauthorized(string),\n" +
               "    forbidden(string),\n" +
               "    not-found(string),\n" +
               "    rate-limit-exceeded(string),\n" +
               "    internal-error(string),\n" +
               "}";
    }

    @Override
    public String getTypeDeclaration(final Schema schema) {
        if (schema == null) {
            return "void";
        }
        
        if (schema instanceof ArraySchema) {
            Schema<?> inner = ((ArraySchema) schema).getItems();
            // Use raw angle brackets instead of HTML entities
            String s = String.format(
                java.util.Locale.ROOT,
                "list<%s>",
                getTypeDeclaration(inner));
                return s;
        }
        
        if (schema instanceof MapSchema) {
            Schema inner = (Schema) schema.getAdditionalProperties();
            return "record {\n" +
                   "    entries: list<tuple<string, " + getTypeDeclaration(inner) + ">>\n" +
                   "}";
        }
        
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return generateEnumDeclaration(schema);
        }
        
        if (schema instanceof ComposedSchema) {
            return handleComposedSchema((ComposedSchema) schema);
        }
        
        String schemaType = getSchemaType(schema);
        String mappedType = typeMapping.get(schemaType);
        return mappedType != null ? mappedType : toModelName(schemaType);
    }

    private String generateEnumDeclaration(Schema schema) {
        @SuppressWarnings("unchecked")
        List<String> enumValues = ((List<Object>) schema.getEnum()).stream()
            .map(Object::toString)
            .map(value -> sanitizeName(value))
            .collect(Collectors.toList());
            
        return "enum " + toModelName(schema.getName()) + " {\n    " +
               String.join(",\n    ", enumValues) + "\n}";
    }

    private String handleComposedSchema(ComposedSchema schema) {
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            return generateVariantType(schema);
        }
        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            return generateRecordType(schema);
        }
        return "record";
    }

    private String generateVariantType(ComposedSchema schema) {
        List<Schema> variants = schema.getOneOf();
        StringBuilder sb = new StringBuilder("variant ").append(toModelName(schema.getName())).append(" {\n");
        
        for (Schema variant : variants) {
            String variantName = sanitizeName(variant.getName());
            String variantType = getTypeDeclaration(variant);
            sb.append("    ").append(variantName).append("(").append(variantType).append("),\n");
        }
        
        return sb.append("}").toString();
    }

    private String generateRecordType(ComposedSchema schema) {
        List<Schema> components = schema.getAllOf();
        StringBuilder sb = new StringBuilder("record ").append(toModelName(schema.getName())).append(" {\n");
        
        for (Schema component : components) {
            Map<String, Schema> properties = component.getProperties();
            if (properties != null) {
                for (Map.Entry<String, Schema> property : properties.entrySet()) {
                    String propertyName = sanitizeName(property.getKey());
                    String propertyType = getTypeDeclaration(property.getValue());
                    sb.append("    ").append(propertyName).append(": ").append(propertyType).append(",\n");
                }
            }
        }
        
        return sb.append("}").toString();
    }

    @Override
    public CodegenModel fromModel(final String name, final Schema schema) {
        CodegenModel model = super.fromModel(name, schema);
        
        // Handle discriminator
        if (schema.getDiscriminator() != null) {
            model.discriminator = schema.getDiscriminator();
            model.vendorExtensions.put("x-has-discriminator", true);
        }

        // Handle properties including references, arrays, maps, and composed schemas
        if (schema.getProperties() != null) {
            Map<String, Schema> properties = schema.getProperties();
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Schema propertySchema = entry.getValue();
                
                Optional<CodegenProperty> property = model.vars.stream()
                    .filter(v -> v.name.equals(propertyName))
                    .findFirst();
                    
                if (property.isPresent()) {
                    processProperty(property.get(), propertySchema);
                }
            }
        }

        // Handle additional properties
        if (schema.getAdditionalProperties() != null) {
            model.hasAdditionalProperties = true;
            if (schema.getAdditionalProperties() instanceof Schema) {
                Schema additionalPropSchema = (Schema) schema.getAdditionalProperties();
                model.vendorExtensions.put("x-additional-property-type", getTypeDeclaration(additionalPropSchema));
            }
        }

        // Handle composition types (allOf, oneOf, anyOf)
        if (schema instanceof ComposedSchema) {
            ComposedSchema composed = (ComposedSchema) schema;
            processComposedSchema(model, composed);
        }

        // Handle enums
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            processEnum(model, schema);
        }

        // Handle nullable properties
        if (Boolean.TRUE.equals(schema.getNullable())) {
            model.vendorExtensions.put("x-is-nullable", true);
        }

        // Handle format specifications
        if (schema.getFormat() != null) {
            model.vendorExtensions.put("x-format", schema.getFormat());
        }

        // Handle patterns
        if (schema.getPattern() != null) {
            model.vendorExtensions.put("x-pattern", schema.getPattern());
        }

        // Handle maximum/minimum constraints
        processNumericConstraints(model, schema);

        // Special handling for response types
        if (name.endsWith("Response")) {
            model.vendorExtensions.put("x-is-response", true);
            handleResponseModel(model, schema);
        }
        
        return model;
    }

    private void processProperty(CodegenProperty prop, Schema propertySchema) {
        if (propertySchema.get$ref() != null) {
            processReference(prop, propertySchema.get$ref());
        } else if (propertySchema instanceof ArraySchema) {
            processArraySchema(prop, (ArraySchema) propertySchema);
        } else if (propertySchema instanceof MapSchema) {
            processMapSchema(prop, (MapSchema) propertySchema);
        } else if (propertySchema instanceof ComposedSchema) {
            processComposedPropertySchema(prop, (ComposedSchema) propertySchema);
        }
        
        // Handle constraints
        if (Boolean.TRUE.equals(propertySchema.getRequired())) {
            prop.required = true;
        }
        if (Boolean.TRUE.equals(propertySchema.getNullable())) {
            prop.vendorExtensions.put("x-is-nullable", true);
        }
    }

    private void processReference(CodegenProperty prop, String ref) {
        String modelName = ref.substring(ref.lastIndexOf('/') + 1);
        prop.dataType = toModelName(modelName);
        prop.complexType = modelName;
        prop.isModel = true;
    }

    private void processArraySchema(CodegenProperty prop, ArraySchema arraySchema) {
        Schema itemsSchema = arraySchema.getItems();
        if (itemsSchema.get$ref() != null) {
            String ref = itemsSchema.get$ref();
            String modelName = ref.substring(ref.lastIndexOf('/') + 1);
            prop.items.dataType = toModelName(modelName);
            prop.items.complexType = modelName;
            prop.items.isModel = true;
        }
    }

    private void processMapSchema(CodegenProperty prop, MapSchema mapSchema) {
        Schema valueSchema = (Schema) mapSchema.getAdditionalProperties();
        if (valueSchema.get$ref() != null) {
            String ref = valueSchema.get$ref();
            String modelName = ref.substring(ref.lastIndexOf('/') + 1);
            prop.items.dataType = toModelName(modelName);
            prop.items.complexType = modelName;
            prop.items.isModel = true;
        }
    }

    private void processComposedSchema(CodegenModel model, ComposedSchema schema) {
        if (schema.getAllOf() != null) {
            model.vendorExtensions.put("x-composed-type", "allOf");
        } else if (schema.getOneOf() != null) {
            model.vendorExtensions.put("x-composed-type", "oneOf");
        } else if (schema.getAnyOf() != null) {
            model.vendorExtensions.put("x-composed-type", "anyOf");
        }
    }

    private void processEnum(CodegenModel model, Schema schema) {
        model.isEnum = true;
        model.dataType = "enum";
        model.allowableValues = new HashMap<>();
        model.allowableValues.put("values", schema.getEnum());
    }

    private void processNumericConstraints(CodegenModel model, Schema schema) {
        if (schema.getMaximum() != null) {
            model.vendorExtensions.put("x-maximum", schema.getMaximum());
        }
        if (schema.getMinimum() != null) {
            model.vendorExtensions.put("x-minimum", schema.getMinimum());
        }
        if (schema.getExclusiveMaximum() != null) {
            model.vendorExtensions.put("x-exclusive-maximum", schema.getExclusiveMaximum());
        }
        if (schema.getExclusiveMinimum() != null) {
            model.vendorExtensions.put("x-exclusive-minimum", schema.getExclusiveMinimum());
        }
    }

    private void handleResponseModel(CodegenModel model, Schema schema) {
        // Extract the actual response type from wrapper types
        if (schema.getProperties() != null && schema.getProperties().containsKey("data")) {
            Schema dataSchema = (Schema) schema.getProperties().get("data");
            model.vendorExtensions.put("x-response-type", getTypeDeclaration(dataSchema));
        }
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(
            final OperationsMap objs,
            final List<ModelMap> allModels) {
        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();
        
        for (CodegenOperation op : operationList) {
            processOperation(op);
        }
        
        return objs;
    }

    private void processOperation(CodegenOperation operation) {
        // Clean up parameter names
        for (CodegenParameter param : operation.allParams) {
            param.paramName = sanitizeName(param.paramName);
            if (param.isEnum) {
                param.dataType = toModelName(param.dataType);
            }
        }
        
        // Handle return types
        String returnType = determineReturnType(operation);
        operation.vendorExtensions.put("x-wit-return", 
            "expected<" + returnType + ", error>");
    }

    private String determineReturnType(CodegenOperation operation) {
        if (operation.returnType == null || operation.returnType.equals("void")) {
            return "void";
        }
        
        // Handle response wrapper types
        if (operation.returnType.endsWith("Response")) {
            String innerType = operation.returnType.substring(0, 
                operation.returnType.length() - "Response".length());
            return toModelName(innerType);
        }
        
        return operation.returnType;
    }

    @Override
    public String getName() {
        return "wit";
    }

    @Override
    public String getHelp() {
        return "Generates WebAssembly Interface Type (WIT) definitions from OpenAPI specifications";
    }

    @Override
    public String toModelName(final String name) {
        if (name == null) {
            return "_empty";
        }
        String sanitized = sanitizeName(name);
        return reservedWords.contains(sanitized) ? 
            escapeReservedWord(sanitized) : StringUtils.camelize(sanitized);
    }

    @Override
    public String sanitizeName(final String name) {
        if (name == null) {
            return "_empty";
        }
        return name.replaceAll("[^a-zA-Z0-9_-]", "-")
                  .replaceAll("-{2,}", "-")
                  .replaceAll("^-|-$", "")
                  .toLowerCase(java.util.Locale.ROOT);
    }

    public String escapeReservedWord(final String name) {
        return name + "_type";
    }
}