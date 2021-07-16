package ru.geekbrains;

import java.util.Optional;

public interface AuthenticationProvider {
    void init();

    Optional<String> getNicknameByLoginAndPassword(String login, String password);

    void changeNickname(String oldNickname, String newNickname);

    boolean isNickBusy(String nickname);

    void shutdown();


}
