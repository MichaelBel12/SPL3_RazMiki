#include <iostream>
#include <thread>
#include <string>
#include <sstream>
#include <vector>
#include "../include/ConnectionHandler.h"
#include "../include/StompClientProtocol.h" // Corrected: Include the .h header

// Global protocol object
StompClientProtocol protocol;

void socketThreadTask(ConnectionHandler* handler) {
    while (true) {
        std::string serverFrame;
        // Assume no network cable disconnection as per instructions
        if (handler->getFrameAscii(serverFrame, '\0')) {
            protocol.processResponse(serverFrame); 
            serverFrame.clear();
        }
    }
}

int main(int argc, char *argv[]) {
    ConnectionHandler* handler = nullptr;
    std::thread* socketThread = nullptr;

    while (true) {
        std::string userInput;
        if (!std::getline(std::cin, userInput)) break;
        std::stringstream ss(userInput);
        std::string command;
        ss >> command;

        if (command == "login") {
            if(handler){
                std::cout << "The client is already logged in, log out before trying again" << std::endl;
                continue;
            }
            else{
            std::string hostPort;
            std::string username;
            std::string password;
            ss >> hostPort;
            ss >> username;
            ss >> password;
            size_t colonPos = hostPort.find(':');
            std::string host = hostPort.substr(0, colonPos);
            short port = std::stoi(hostPort.substr(colonPos + 1)); //from string t int
            handler = new ConnectionHandler(host, port);
            if (handler->connect()) {
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
            
        } 
        else if (handler) {
            std::string stompFrame = protocol.processInput(userInput);
            if (!stompFrame.empty()) {
                handler->sendFrameAscii(stompFrame, '\0');
            }
        }
        else{
            std::cout << "You must log in first." << std::endl;
        }
    }


    if (socketThread && socketThread->joinable()) socketThread->join();
    delete handler;
    return 0;
}