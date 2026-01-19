#include <iostream>
#include <thread>
#include <string>
#include <sstream>
#include <vector>
#include "../include/ConnectionHandler.h"
#include "../include/StompClientProtocol.h" 

StompClientProtocol protocol;
bool isConnected = false; // Tracks the state of the network connection

void socketThreadTask(ConnectionHandler* handler) {
    while (isConnected) {
        std::string serverFrame;
        if (handler->getFrameAscii(serverFrame, '\0')) {
            if (!protocol.processResponse(serverFrame)) {
                isConnected = false; 
                break;
            }
            serverFrame.clear();
        } else {
            isConnected = false;
            break;
        }
    }
}

int main(int argc, char *argv[]) {
    ConnectionHandler* handler = nullptr;
    std::thread* socketThread = nullptr;
    std::string userInput;
    while (std::getline(std::cin, userInput)) {
        if (userInput.empty()) continue;
        std::stringstream ss(userInput);
        std::string command;
        ss >> command;
        if (handler != nullptr && !isConnected) {
            if (socketThread && socketThread->joinable()) {
                socketThread->join();
            }
            std::thread* temp=socketThread;
            delete temp;
            socketThread = nullptr;
            ConnectionHandler* tempHand=handler;
            delete tempHand;
            handler = nullptr;
            protocol.clear(); // clearing protocol data on disconnection
        }

        if (command == "login") {
            if (handler != nullptr) {
                std::cout << "The client is already logged in, log out before trying again" << std::endl;
                continue;
            }
            std::string hostPort, username, password;
            ss >> hostPort >> username >> password;
            size_t colonPos = hostPort.find(':');
            std::string host = hostPort.substr(0, colonPos);
            short port = std::stoi(hostPort.substr(colonPos + 1));
            handler = new ConnectionHandler(host, port);
            if (handler->connect()) {
                isConnected = true; 
                protocol.setUserName(username);

                
                std::string connectFrame = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\n"
                                           "login:" + username + "\npasscode:" + password + "\n\n";
                handler->sendFrameAscii(connectFrame, '\0');

                
                socketThread = new std::thread(socketThreadTask, handler);
            } else {
                std::cout << "Could not connect to server" << std::endl;
                delete handler;
                handler = nullptr;
            }
        } 
        else if (handler != nullptr && isConnected) {
            std::vector<std::string> stompFrameVec = protocol.processInput(userInput);  // Send join, exit, report, summary, or logout to the protocol for processing
            for (std::string s : stompFrameVec) {
                handler->sendFrameAscii(s, '\0');
            }
        } 
        else {
            std::cout << "You must log in first." << std::endl;
        }
    }

    // Final cleanup when exits
    isConnected = false;
    if (socketThread && socketThread->joinable()) {
        socketThread->join();
    }
    delete socketThread;
    delete handler;

    return 0;
}