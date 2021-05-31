package project_chat;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class Servidor extends Thread {

    //Tamanho Inicial da Lista
    int initialSize = 16;

    //Valor do Load Factor
    double loadFactor = 0.75;

    /*Multiplicando o tamanho inicial pelo load factor temos 
    * um valor que corresponde ao tamanho máximo suportado pela lista.
    *
    * No nosso caso o resultado será igual a 12. Isso significa
    * que ao inserirmos 12 elementos em nosso HashMap,
    * a lista dobrará de tamanho, ou seja, terá tamanho = 32.
    *
    * Depois o load factor é calculado novamente para o
    * novo tamanho (32) e assim sucessivamente.*/
    double sizeToRehash = initialSize * loadFactor;

    //Uma matriz de String (ip cliente/nome)
    private static Map<String, PrintStream> clientes;
    //socket do server
    private static ServerSocket server;
    //nome do cliente conectado
    private String nome_cliente;
    //socket de nova conexao estabelecida
    private Socket conexao;
    //
    private BufferedReader entrada;
    //
    private PrintStream saida;
            
    public static void main(String args[]) {
        try {
            //Cria os objetos necessário para instânciar o servidor
            JLabel lblMessage = new JLabel("Porta do Servidor:");
            JTextField txtPorta = new JTextField("7777");
            Object[] texts = {lblMessage, txtPorta};
            JOptionPane.showMessageDialog(null, texts);

            server = new ServerSocket(Integer.parseInt(txtPorta.getText()));

            clientes = new HashMap<String, PrintStream>();

            JOptionPane.showMessageDialog(null, "Servidor ativo na porta: " + txtPorta.getText());

            while (true) {
                System.out.println("Aguardando conexão...");
                Socket con = server.accept();
                //System.out.println("Cliente conectado...");
                Thread t = new Servidor(con);
                System.out.println("oi1");
                t.start();
                System.out.println("oi1");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    /**
     * Método run (roda quando da start na thread)
     */
    @Override
    public void run() {

        try {
            System.out.println("oiT");
            //variavel de entrada de dados
            entrada = new BufferedReader(new InputStreamReader(this.conexao.getInputStream()));
            //variavel de saida de dados
            saida = new PrintStream(this.conexao.getOutputStream());

            System.out.println("oiT2");
            this.nome_cliente = entrada.readLine();
            System.out.println(this.nome_cliente);
            System.out.println("oiT3");
            
            int status;
            if (!armazena_cliente(this.nome_cliente, saida)) {
                System.out.println("Erro ao conectar.");
                //saida.println("Este nome ja existe! Conecte novamente com outro Nome.");
                status = 0;
                saida.print(status);
                this.conexao.close();
                return;
            } else {
                //System.out.println(nome_cliente + " conectado com sucesso");
                status = 1;
                saida.print(status);   
                
                String conectados = "";
                saida.println("Conectados:");
                for (String aux : clientes.keySet()) {
                    if (!aux.equalsIgnoreCase(this.nome_cliente)) {
                        saida.println(aux);
                    }
                }

                //envia lista para todos assim que qualquer cliente se conecta
                sendListToAll(this.nome_cliente, 1);
            }

            if (this.nome_cliente == null) {
                return;
            }

            String mensagem = entrada.readLine();
            String nome_destinatario = entrada.readLine();
            
            while(!mensagem.equalsIgnoreCase("Sair") && mensagem != null){
                if (buscar_cliente(nome_destinatario) && !nome_destinatario.equalsIgnoreCase("Sair")){
                    enviar(mensagem, nome_destinatario);
                }
                else{
                    enviar("SERVIDOR -> Cliente (" + nome_destinatario + " ) não encontrado", nome_cliente);
                }
                mensagem = entrada.readLine();
                nome_destinatario = entrada.readLine();
            }

            System.out.println(this.nome_cliente + " saiu do bate-papo!");
            sendListToAll(this.nome_cliente, 0);
            remover(this.nome_cliente);

            this.conexao.close();
//            
//            if (buscar_cliente(nome_destinatario) && !nome_destinatario.equalsIgnoreCase("Sair")) {
//                status = 1;
//                saida.print(status);  
//                //saida.println("Usuário o qual deseja se conectar foi encontrado!");
//
//                String mensagem = entrada.readLine();
//                while (!mensagem.equalsIgnoreCase("Sair") && mensagem != null) {
//                    enviar(mensagem, nome_destinatario);
//                    mensagem = entrada.readLine();
//                }
//
//                System.out.println(this.nome_cliente + " saiu do bate-papo!");
//                sendListToAll(this.nome_cliente, 0);
//                remover(this.nome_cliente);
//
//                this.conexao.close();
//            } else {
//                status = 0;
//                saida.println("Sair");
//                //saida.println("Usuário o qual deseja se conectar não existe!");
//                this.conexao.close();
//                return;
//            }

        } catch (IOException e) {
            System.out.println("Falha na Conexao... .. ." + " IOException: " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Método construtor
     * @param conexao
     */
    //realiza uma nova conexao
    public Servidor(Socket conexao) {
        this.conexao = conexao;
    }

    public boolean armazena_cliente(String newName, PrintStream saida) {

        if (clientes.containsKey(newName)) {
            saida.println("Esse nome já está cadastrado (" + clientes.get(newName) + " )");
            return false;
        } else {
            clientes.put(newName, saida);
            System.out.println(newName + " conectado com sucesso(" + newName + " : " + this.conexao.getInetAddress() + " )");
            //saida.println("Cliente conectado no servidor (" + newName + " : " + this.conexao.getInetAddress() + " )");
            return true;
        }

    }

    public void remover(String oldName) {
        clientes.remove(oldName);
    }

    public boolean buscar_cliente(String name) {
        return clientes.containsKey(name);
    }

    public void sendListToAll(String name_cliente, int acao) {

        PrintStream chat;
        for (Map.Entry<String, PrintStream> cliente : clientes.entrySet()) {

            chat = cliente.getValue();

            if (!name_cliente.equalsIgnoreCase(cliente.getKey())) {
                if (acao == 1) {
                    chat.print(name_cliente + " entrou.\n");
                } else {
                    chat.print(name_cliente + " saiu.\n");
                }
            }

            chat.flush();
        }
    }

    /**
     * Se o array da msg tiver tamanho igual a 1, entao envia para todos Se o
     * tamanho for 2, envia apenas para o cliente escolhido
     * @param msg
     * @param destinatario
     */
    public void enviar(String msg, String destinatario) {

        
        PrintStream chat;
        for (Map.Entry<String, PrintStream> cliente : clientes.entrySet()) {

            chat = cliente.getValue();

            if (destinatario.equalsIgnoreCase(cliente.getKey())) {
                chat.println(this.nome_cliente + ":" + destinatario + " -> " + msg);
                chat.flush();
                break;
            }
        }    
        
        
//        for (Map.Entry<String, PrintStream> cliente : clientes.entrySet()) {
//
//            if (destinatario.equalsIgnoreCase(cliente.getKey())) {
//                PrintStream chat = cliente.getValue();
//                chat.println(this.nome_cliente + " enviou: " + msg);
//            }
//            break;
//        }
    } 

}//Fim da classe

