#include "../include/StompClientProtocol.h"
#include <iostream>
#include <sstream>
#include "../include/event.h"
#include <fstream>
#include <map>
#include <algorithm>
std::map<std::string, int> map;
int counterID=0;
std::string myUsername;
std::map<std::pair<std::string,std::string>,std::vector<Event>> history; //key: channel,senderName  value:his events

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
                tosend+="team a updates:\n";
                for (const auto& pair : e.get_team_a_updates()) {
                    tosend += "    "+pair.first + ": " + pair.second + "\n";
                }
                tosend+="team b updates:\n";
                for (const auto& pair : e.get_team_b_updates()) {
                    tosend += "    "+pair.first + ": " + pair.second + "\n";
                }
                tosend+="description:\n"+e.get_discription();
                output.push_back(tosend);
            }
    }
    else if(command=="summary"){
            std::string toSend;
            std::string game_name;
            std::string user;
            std::string file;
            ss>>game_name;
            ss>>user;
            ss>>file;
            int indexOfUnderscore=game_name.find('_');
            std::string teamA=game_name.substr(0,indexOfUnderscore);
            std::string teamB=game_name.substr(indexOfUnderscore+1);
            std::pair<std::string,std::string> toCheck(game_name,user);
            if(history.count(toCheck)>0){
                std::ofstream outFile(file);
                std::vector<Event>& eventsToWrite= history[toCheck];
                std::vector<std::pair<std::string, std::string>> general_stats_vec;
                std::vector<std::pair<std::string, std::string>> team_a_stats_vec;
                std::vector<std::pair<std::string, std::string>> team_b_stats_vec;
                std::vector<std::string> game_reports_and_desc_vec;
        

                for(Event e:eventsToWrite){
                    std::map<std::string, std::string> generalupdatesmap=e.get_game_updates();
                    for(const auto& [key, value] : generalupdatesmap){
                        removeIfKeyExists(general_stats_vec,key);      //ensures us we gonna print only the latest updates for each stat
                        general_stats_vec.push_back({key,value});
                    }
                    std::map<std::string, std::string> teamAUpdates=e.get_team_a_updates();
                    for(const auto& [key, value] : teamAUpdates){
                        removeIfKeyExists(team_a_stats_vec,key); //ensures us we gonna print only the latest updates for each stat
                        team_a_stats_vec.push_back({key,value});
                    }
                    std::map<std::string, std::string> teamBUpdates=e.get_team_b_updates();
                    for(const auto& [key, value] : teamBUpdates){
                        removeIfKeyExists(team_b_stats_vec,key); //ensures us we gonna print only the latest updates for each stat
                        team_b_stats_vec.push_back({key,value});
                    }
                    game_reports_and_desc_vec.push_back(std::to_string(e.get_time()) +" - "+ e.get_name()+":\n\n"+e.get_discription()+"\n");
                }  
                std::sort(general_stats_vec.begin(),general_stats_vec.end(),comparePairs);
                std::sort(team_a_stats_vec.begin(),team_a_stats_vec.end(),comparePairs);
                std::sort(team_b_stats_vec.begin(),team_b_stats_vec.end(),comparePairs);
                outFile<< teamA+" vs "+teamB+"\n";              
                outFile<< "Game stats: \nGeneral stats:\n";
                for(std::pair<std::string,std::string>& p : general_stats_vec){
                    outFile<<p.first+": "+p.second+"\n";
                }
                outFile<<"\n"+teamA+" stats:\n";
                for(std::pair<std::string,std::string>& p : team_a_stats_vec){
                    outFile<<p.first+": "+p.second+"\n";
                }
                outFile<<"\n"+teamB+" stats:\n";
                for(std::pair<std::string,std::string>& p : team_b_stats_vec){
                    outFile<<p.first+": "+p.second+"\n";
                }
                outFile << "\nGame event reports:\n";
                for(std::string s:game_reports_and_desc_vec){
                    outFile<< s;
                }
            }
            else{
                std::cout<<"User Hasn't received any game updates from given user"<<std::endl;
                //put in the file the error too
            }
    }
    else if(command=="logout"){
        std::string toSend="DISCONNECT\nreceipt:"+counterID++;
        output.push_back(toSend);
    }

    return output;
}

bool StompClientProtocol::processResponse(std::string frame) {

















    return true;
}

void setUserName(std::string username){
    myUsername=username;
}
bool comparePairs(const std::pair<std::string, std::string>& a, 
                  const std::pair<std::string, std::string>& b) {
    // Return true if 'a' is lexicographically smaller than 'b'
    return a.first < b.first;
}
void removeIfKeyExists(std::vector<std::pair<std::string, std::string>>& vec, std::string key) {
    auto it = vec.begin();
    while (it != vec.end()) {
        if (it->first == key) {
            it = vec.erase(it); 
            break;
        } 
        else
         {
           it++; 
        }
    }
}