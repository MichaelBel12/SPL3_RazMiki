#pragma once
#include <string>
#include <vector>  
#include <utility> 
class StompClientProtocol {
public:
    // Translates keyboard commands into raw STOMP frames
    std::vector<std::string> processInput(std::string input); 

    // Parses server responses and prints relevant information
    bool processResponse(std::string frame);  
    
    void setUserName(std::string username);

    static bool comparePairs(const std::pair<std::string, std::string>& a,const std::pair<std::string, std::string>& b); //changed to static for the compare

    void removeIfKeyExists(std::vector<std::pair<std::string, std::string>>& vec, std::string key);

    void clear();

    static bool comparebytime(const std::pair<std::string, int>& a,const std::pair<std::string, int>& b);


};