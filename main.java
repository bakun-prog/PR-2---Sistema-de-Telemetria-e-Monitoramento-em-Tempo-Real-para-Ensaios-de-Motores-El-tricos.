// Define o pacote da aplicação (organização do projeto)
package org.example;

// Importa a biblioteca Eclipse Paho MQTT (cliente MQTT para Java)
import org.eclipse.paho.client.mqttv3.*;

public class Main {

    // Endereço do broker HiveMQ Cloud usando TLS (conexão segura)
    // Porta 8883 = padrão MQTT com SSL/TLS
    static String broker = "ssl://ceea9a2bd525486b96a63c0ba0da8708.s1.eu.hivemq.cloud:8883";

    // Credenciais de autenticação
    static String username = "Pietro";
    static String password = "Aa123456";

    // Tópicos onde o Java vai ESCUTAR os dados dos sensores
    static String topicTemp = "senai/Pietro/temperatura";
    static String topicCorrente = "senai/Pietro/corrente";
    static String topicVibracao = "senai/Pietro/vibracao";

    // Guardam os últimos valores recebidos
    static double ultimaTemp = 0;
    static double ultimaCorrente = 0;
    static double ultimaVibracao = 0;

    // Servem para saber se os dados já chegaram
    static boolean tempRecebida = false;
    static boolean correnteRecebida = false;
    static boolean vibracaoRecebida = false;

    public static void main(String[] args) throws Exception {

        // Cria o cliente MQTT
        // "JavaSupervisorio" é o Client ID (identificação no broker)
        MqttClient client = new MqttClient(broker, "JavaSupervisorio");

        // Configura opções de conexão
        MqttConnectOptions options = new MqttConnectOptions();

        // Reconecta automaticamente se cair
        options.setAutomaticReconnect(true);

        // Inicia sessão limpa (não mantém mensagens antigas)
        options.setCleanSession(true);

        // 🔐 Define autenticação
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        System.out.println("Conectando ao HiveMQ Cloud com TLS...");

        // Conecta ao broker usando as opções configuradas
        client.connect(options);

        System.out.println("✅ Conectado com sucesso via TLS!");

        // Inscreve o cliente nos 3 tópicos ao mesmo tempo
        client.subscribe(new String[]{
                topicTemp,
                topicCorrente,
                topicVibracao
        });

        // Define o que acontece quando eventos MQTT ocorrerem
        client.setCallback(new MqttCallback() {

            // Executado automaticamente quando chega mensagem
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                // Converte os bytes recebidos em String
                String valor = new String(message.getPayload());

                // Se a mensagem veio do tópico de temperatura
                if (topic.equals(topicTemp)) {
                    ultimaTemp = Double.parseDouble(valor); // converte para número
                    tempRecebida = true; // marca como recebida
                }

                // Se veio do tópico de corrente
                if (topic.equals(topicCorrente)) {
                    ultimaCorrente = Double.parseDouble(valor);
                    correnteRecebida = true;
                }

                // Se veio do tópico de vibração
                if (topic.equals(topicVibracao)) {
                    ultimaVibracao = Double.parseDouble(valor);
                    vibracaoRecebida = true;
                }

                // Só processa quando os 3 valores forem recebidos
                if (tempRecebida && correnteRecebida && vibracaoRecebida) {

                    // Executa a lógica do supervisório
                    processarLogica(client);

                    // Reseta as flags para esperar novos dados
                    tempRecebida = false;
                    correnteRecebida = false;
                    vibracaoRecebida = false;
                }
            }

            // Executado se a conexão cair
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("❌ Conexão perdida!");
                cause.printStackTrace();
            }

            // Executado quando uma mensagem publicada é entregue
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Não está sendo usado aqui
            }
        });
    }

    private static void processarLogica(MqttClient client) throws MqttException {

        // Formata linha 1 para o LCD (Temperatura)
        String linha1 = "T:" + String.format("%.1f", ultimaTemp) + "C";

        // Formata linha 2 para o LCD (Corrente + Vibração)
        String linha2 = "I:" + String.format("%.1f", ultimaCorrente) +
                "A V:" + String.format("%.1f", ultimaVibracao) + "%";

        // Junta as duas linhas separadas por quebra de linha
        String mensagemLCD = linha1 + "\n" + linha2;

        // Cria mensagem MQTT convertendo texto para bytes
        MqttMessage msg = new MqttMessage(mensagemLCD.getBytes());

        // QoS 1 = entrega garantida (pelo menos uma vez)
        msg.setQos(1);

        // Publica no tópico que o ESP32 está escutando
        client.publish("senai/Pietro/comando/lcd", msg);

        // Mostra os valores no console do Java
        System.out.println(
                "Temp: " + ultimaTemp +
                        "°C | Corrente: " + ultimaCorrente +
                        "A | Vibracao: " + ultimaVibracao + "%"
        );
    }
}