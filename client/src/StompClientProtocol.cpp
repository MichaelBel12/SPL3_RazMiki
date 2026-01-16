#include "../include/StompClientProtocol.h"
#include <iostream>
#include <sstream>
#include <map>
std::map<std::string, int> map;
int counterID=0;

std::string StompClientProtocol::processInput(std::string input) {
    std::string userInput;
    std::getline(std::cin, userInput);
    std::stringstream ss(userInput);
    std::string command;
    ss >> command;
    if(command=="join"){
        std::string game_name; //we assume its legal according to the pdf
        ss>>game_name;
        std::string toSend="SUBSCRIBE\ndestination:/"+game_name+"\nid:"+std::to_string(counterID++)+"\nreceipt:"+std::to_string(counterID)+"\n\n";
        return toSend;
    }
    
















    return ""; 
}

bool StompClientProtocol::processResponse(std::string frame) {
    if (frame.find("CONNECTED") == 0) {
        std::cout << "Login successful" << std::endl;
    } else {
        std::cout << "Server sent: " << frame << std::endl;
    }
}