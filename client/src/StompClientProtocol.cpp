#include "../include/StompClientProtocol.h"
#include <iostream>
#include <sstream>
#include "../include/event.h"
#include <map>
std::map<std::string, int> map;
int counterID=0;
std::string myUsername;


std::vector<std::string> StompClientProtocol::processInput(std::string input) {
    std::vector<std::string> output;
    std::string userInput;
    std::getline(std::cin, userInput);
    std::stringstream ss(userInput);
    std::string command;
    ss >> command;
    if(command=="join"){
        std::string game_name; //we assume its legal according to the pdf
        ss>>game_name;
        std::string toSend="SUBSCRIBE\ndestination:/"+game_name+"\nid:"+std::to_string(counterID++)+"\nreceipt:"+std::to_string(counterID)+"\n\n";
        map[game_name]=counterID;
        output.push_back(toSend);
    }
    else if(command=="exit"){
        std::string game_name; //we assume its legal according to the pdf
        ss>>game_name;
        if (map.count(game_name)>0) {  
        int idToRemove=map[game_name];
        map.erase(game_name);
        std::string toSend="UNSUBSCRIBE\nid:"+std::to_string(idToRemove)+"receipt:"+std::to_string(counterID++);
        output.push_back(toSend);
        } 
    }
    else if(command=="report"){
        std::string json;
        ss>>json;
        names_and_events curEventNames= parseEventsFile(json);
        std::string game_name= curEventNames.team_a_name +"_"+ curEventNames.team_b_name;
        for(Event e:curEventNames.events){
            std::string tosend="SEND\ndestination:/"+game_name+"\n\nuser: "+myUsername+"team a: "+curEventNames.team_a_name+"\nteam b: "+curEventNames.team_b_name
            +"event name: "+e.get_name()+"\ntime: "+std::to_string(e.get_time())+"general game updates: \n";
            for (const auto& pair : e.get_game_updates()) {
                tosend += "    "+pair.first + ": " + pair.second + "\n";
            }   
            










            output.push_back(toSend);
        }










        return output;
    }
    return output;
}

bool StompClientProtocol::processResponse(std::string frame) {
    if (frame.find("CONNECTED") == 0) {
        std::cout << "Login successful" << std::endl;
    } else {
        std::cout << "Server sent: " << frame << std::endl;
    }
}

void setUserName(std::string username){
    myUsername=username;
}