#pragma once
#include <string>

class StompClientProtocol {
public:
    // Translates keyboard commands into raw STOMP frames
    std::vector<std::string> processInput(std::string input); 

    // Parses server responses and prints relevant information
    bool processResponse(std::string frame);  
    
    void setUserName(std::string username);
};