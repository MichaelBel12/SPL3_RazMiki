
#include "../include/StompClientProtocol.h"
#include <iostream>
#include <sstream>
#include "../include/event.h"
#include <fstream>
#include <map>
#include <algorithm>
#include <string>
#include <random>


std::map<std::string, int> map_ChannelWithSubID;
int counterID=100;
std::string myUsername;
std::map<std::pair<std::string,std::string>,std::vector<Event>> history; //key: channel,senderName  value:his events
std::map<int,std::string> receiptMap;

std::vector<std::string> StompClientProtocol::processInput(std::string input) {
    std::vector<std::string> output;
    std::stringstream ss(input);
    std::string command;
    ss >> command;
    std::cout << "command: "+ command <<std::endl;
    if(command=="join"){
        std::string game_name; //we assume its legal according to the pdf
        ss>>game_name;
        counterID++;
        int idTosend=counterID;
        if(map_ChannelWithSubID.count(game_name)>0){
            idTosend=map_ChannelWithSubID[game_name];
        }
        std::string toSend="SUBSCRIBE\ndestination:/"+game_name+"\nid:"+std::to_string(idTosend)+"\nreceipt:"+std::to_string(counterID)+"\n\n";
        map_ChannelWithSubID[game_name]=idTosend;
        output.push_back(toSend);
        receiptMap[counterID]="Joined channel "+game_name;
    }
    else if(command=="exit"){
        std::string game_name; //we assume its legal according to the pdf
        ss>>game_name;
        if (map_ChannelWithSubID.count(game_name)>0) {  
        int idToRemove=map_ChannelWithSubID[game_name];
        map_ChannelWithSubID.erase(game_name);
        counterID++;
        std::string toSend="UNSUBSCRIBE\nid:"+std::to_string(idToRemove)+"\nreceipt:"+std::to_string(counterID)+"\n\n";
        output.push_back(toSend);
        receiptMap[counterID]="Exited channel "+game_name;
        } 
        else{
        counterID++;
        std::string toSend = "UNSUBSCRIBE\nid:0\nreceipt:" + std::to_string(counterID) + "\n\n";  //"faking a wrong ubsubscribe just to get the error frame"
        output.push_back(toSend);
        receiptMap[counterID] = "Attempted to exit non-existent channel " + game_name;
        }
        
    
    }
    else if(command=="report"){
            std::string json;
            ss>>json;
            names_and_events curEventNames= parseEventsFile(json);
            std::string game_name= curEventNames.team_a_name +"_"+ curEventNames.team_b_name;
            for(Event e:curEventNames.events){
                std::string tosend="SEND\ndestination:/"+game_name+"\n\nuser: "+myUsername+"\nteam a: "+curEventNames.team_a_name+"\nteam b: "+curEventNames.team_b_name
                +"\nevent name: "+e.get_name()+"\ntime: "+std::to_string(e.get_time())+"\ngeneral game updates: \n";
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
                tosend+="description:\n"+e.get_discription()+"\n";
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
                std::vector<std::pair<std::string, int>> game_reports_and_desc_vec_beforeHalftime;
                std::vector<std::pair<std::string, int>> game_reports_and_desc_vec_afterHalftime;
                bool halftimePassed=false;
        

                for(Event e:eventsToWrite){
                    if(e.get_name()=="Halftime"){
                        halftimePassed=true;
                    }
                    std::map<std::string, std::string> generalupdatesmap=e.get_game_updates();
                    for(const auto& pair : generalupdatesmap) {
                        if(pair.first=="before halftime"){
                            if(pair.second=="true")
                            {halftimePassed=false;}
                            else{halftimePassed=true;}

                        }
                        const std::string& key = pair.first;
                        const std::string& value = pair.second;
                        removeIfKeyExists(general_stats_vec, key);
                        general_stats_vec.push_back({key, value});
                    }
                    std::map<std::string, std::string> teamAUpdates = e.get_team_a_updates();
                    for(const auto& pair : teamAUpdates) {
                        const std::string& key = pair.first;
                        const std::string& value = pair.second;
                        removeIfKeyExists(team_a_stats_vec, key); 
                        team_a_stats_vec.push_back({key, value});
                    } 
                    std::map<std::string, std::string> teamBUpdates = e.get_team_b_updates();
                    for(const auto& pair : teamBUpdates) {
                        const std::string& key = pair.first;
                        const std::string& value = pair.second;
                        removeIfKeyExists(team_b_stats_vec, key);
                        team_b_stats_vec.push_back({key, value});
                    }
                    int timeOfEvent=e.get_time();

                    if (halftimePassed){
                        game_reports_and_desc_vec_afterHalftime.push_back({std::to_string(e.get_time()) +" - "+ e.get_name()+":\n\n"+e.get_discription()+"\n",timeOfEvent});
                    }
                    else{
                        game_reports_and_desc_vec_beforeHalftime.push_back({std::to_string(e.get_time()) +" - "+ e.get_name()+":\n\n"+e.get_discription()+"\n",timeOfEvent});
                    }
                    
                } 
                std::sort(game_reports_and_desc_vec_beforeHalftime.begin(),game_reports_and_desc_vec_beforeHalftime.end(),comparebytime);
                std::sort(game_reports_and_desc_vec_afterHalftime.begin(),game_reports_and_desc_vec_afterHalftime.end(),comparebytime);
                std::sort(general_stats_vec.begin(),general_stats_vec.end(),comparePairs);
                std::sort(team_a_stats_vec.begin(),team_a_stats_vec.end(),comparePairs);
                std::sort(team_b_stats_vec.begin(),team_b_stats_vec.end(),comparePairs);
                outFile<< teamA+" vs "+teamB+"\n";              
                outFile<< "Game stats: \nGeneral stats:\n";
                for(std::pair<std::string,std::string>& p : general_stats_vec){
                    outFile<<"    "+p.first+": "+p.second+"\n";
                }
                outFile<<"\n"+teamA+" stats:\n";
                for(std::pair<std::string,std::string>& p : team_a_stats_vec){
                    outFile<<"    "+p.first+": "+p.second+"\n";
                }
                outFile<<"\n"+teamB+" stats:\n";
                for(std::pair<std::string,std::string>& p : team_b_stats_vec){
                    outFile<<"    "+p.first+": "+p.second+"\n";
                }
                outFile << "\nGame event reports:\n";
                for(std::pair<std::string, int> s:game_reports_and_desc_vec_beforeHalftime){
                    outFile<< s.first+"\n";
                }
                outFile << "\nGame event reports:\n";
                for(std::pair<std::string, int> s:game_reports_and_desc_vec_afterHalftime){
                    outFile<< s.first+"\n";
                }
            }
            else{
                std::cout<<"User Hasn't received any game updates from given user"<<std::endl;
                //put in the file the error too
            }
    }
    else if(command=="logout"){
        counterID++;
        std::string toSend="DISCONNECT\nreceipt:"+std::to_string(counterID)+"\n\n";
        output.push_back(toSend);
       receiptMap[counterID]="logout"; 
    }

    return output;
}

bool StompClientProtocol::processResponse(std::string frame) {
    std::cout <<frame<< std::endl;
    std::stringstream ss(frame); 
    std::string command;
    ss >> command;
    if(command=="ERROR"){
         std::cout<< frame << std::endl;
        return false;
    }
    else if(command=="CONNECTED"){
        std::cout<< "Login successful" << std::endl;
        return true;
    }
    else if(command=="RECEIPT"){
       std::string header;
       ss >> header; 
       std::string id = header.substr(header.find(':') + 1);
       std::string toSend=receiptMap[std::stoi(id)];
       if(toSend=="logout"){
        receiptMap.erase(std::stoi(id));
        std::cout << "logging out of server- Bye!" << std::endl;  
        return false;
       }
       else (std::cout << toSend << std::endl);
       return true;
       
    }
    else if (command == "MESSAGE") {
    std::string line, destination, body;
     std::getline(ss, line);
    while (std::getline(ss, line) && line != "" && line != "\r") {  //read untill empty line is found
        if (line.substr(0, 13) == "destination:/") {
            destination = line.substr(13); //extracting the game name
        }
    }
    while (std::getline(ss, line)) {
        body += line + "\n";          //parsing the body (report) into 1 string
    }
    std::stringstream bodySS(body);    //first line of body is always the sender 
    std::string userLine;
    std::getline(bodySS, userLine);
    std::string sender = userLine.substr(6);   //prase after "user: "
    Event newEvent(body);                    //new builder
    history[{destination, sender}].push_back(newEvent);
    return true;
}

    return false;
}

void StompClientProtocol::setUserName(std::string username){
    myUsername=username;
}
bool StompClientProtocol::comparePairs(const std::pair<std::string, std::string>& a, 
                  const std::pair<std::string, std::string>& b) {
    // Return true if 'a' is lexicographically smaller than 'b'
    return a.first < b.first;
}
void StompClientProtocol::removeIfKeyExists(std::vector<std::pair<std::string, std::string>>& vec, std::string key) {
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
void StompClientProtocol::clear(){
    map_ChannelWithSubID.clear();
    receiptMap.clear();
    history.clear();
}
bool StompClientProtocol::comparebytime(const std::pair<std::string, int>& a, 
                  const std::pair<std::string, int>& b){
    return a.second < b.second;
                  }