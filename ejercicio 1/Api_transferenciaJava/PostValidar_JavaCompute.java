import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbPolicy;
import com.ibm.broker.plugin.MbUserException;

import java.util.UUID;

public class PostValidar_JavaCompute extends MbJavaComputeNode {

    private static final String POLICY_NAME =
        "{Api_transferencia_Policies}:PolicyAutenticacion";

    public void evaluate(MbMessageAssembly inAssembly) throws MbException {
        MbOutputTerminal out = getOutputTerminal("out");

        try {
            MbMessage outMessage = new MbMessage(inAssembly.getMessage());
            MbMessageAssembly outAssembly =
                new MbMessageAssembly(inAssembly, outMessage);

            MbMessage environment = outAssembly.getGlobalEnvironment();
            MbElement environmentRoot = environment.getRootElement();

            MbElement variables =
                environmentRoot.getFirstElementByPath("Variables");

            if (variables == null) {
                variables = environmentRoot.createElementAsLastChild(
                    MbElement.TYPE_NAME,
                    "Variables",
                    null
                );
            }

            // Generar el correlationId.
            setVariable(
                variables,
                "correlationId",
                UUID.randomUUID().toString()
            );

            // Leer la User-Defined Policy.
            MbPolicy policy = MbPolicy.getPolicy(
                "UserDefined",
                POLICY_NAME
            );

            if (policy == null) {
                throw new Exception(
                    "No se encontro la politica " + POLICY_NAME
                );
            }

            String tokenEndpoint =
                policy.getPropertyValueAsString("tokenEndpoint");

            String clientId =
                policy.getPropertyValueAsString("clientId");

            String timeoutMsText =
                policy.getPropertyValueAsString("timeoutMs");

            if (tokenEndpoint == null || tokenEndpoint.trim().isEmpty()) {
                throw new Exception(
                    "La propiedad tokenEndpoint no esta configurada"
                );
            }

            if (clientId == null || clientId.trim().isEmpty()) {
                throw new Exception(
                    "La propiedad clientId no esta configurada"
                );
            }

            if (timeoutMsText == null || timeoutMsText.trim().isEmpty()) {
                throw new Exception(
                    "La propiedad timeoutMs no esta configurada"
                );
            }

            int timeoutMs;

            try {
                timeoutMs = Integer.parseInt(timeoutMsText);
            } catch (NumberFormatException e) {
                throw new Exception(
                    "timeoutMs debe ser un numero entero: " + timeoutMsText
                );
            }

            if (timeoutMs <= 0) {
                throw new Exception(
                    "timeoutMs debe ser mayor que cero"
                );
            }

            // Exponer la configuracion al ESQL.
            setVariable(variables, "tokenEndpoint", tokenEndpoint);
            setVariable(variables, "clientId", clientId);
            setVariable(variables, "timeoutMs", timeoutMs);

            out.propagate(outAssembly);

        } catch (MbException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MbUserException(
                this,
                "evaluate()",
                "",
                "",
                e.getMessage(),
                null
            );
        }
    }

    private void setVariable(
        MbElement variables,
        String name,
        Object value
    ) throws MbException {

        MbElement element = variables.getFirstElementByPath(name);

        if (element == null) {
            variables.createElementAsLastChild(
                MbElement.TYPE_NAME_VALUE,
                name,
                value
            );
        } else {
            element.setValue(value);
        }
    }
}