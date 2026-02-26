package br.com.senai.automacao;

import org.eclipse.paho.client.mqttv3.*;
import org.json.*;

public class App {
      public static void main(String[] args) {
            String broker = "tcp://broker.hivemq.com:1883";
            String clientId = "JavaBackendClient_" + System.currentTimeMillis();

            try {
                MqttClient client = new MqttClient(broker, clientId);
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);

                client.setCallback(new MqttCallback() {
                    public void connectionLost(Throwable cause) {
                        System.out.println("Conexão perdida: " + cause.getMessage());
                    }

                    public void messageArrived(String topic, MqttMessage message) {
                        String payload = new String(message.getPayload());
                        System.out.println("\n--- NOVO DADO RECEBIDO ---");
                        System.out.println("Tópico: " + topic);
                        System.out.println("Valor: " + payload);

                        // Aqui você poderia salvar no banco de dados futuramente
                    }

                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                client.connect(options);
                // Assinando os tópicos do motor
                client.subscribe("motor/#");

                System.out.println("Backend rodando e aguardando dados do motor...");

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
