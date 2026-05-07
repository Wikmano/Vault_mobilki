STRUKTURA PROJEKTU - VAULT APP (MVVM ARCHITECTURE)

Niniejszy projekt został zrefaktoryzowany zgodnie z architekturą MVVM (Model-View-ViewModel), co pozwala na lepszą separację logiki biznesowej od interfejsu użytkownika.

1. PODZIAŁ KATALOGÓW:

- com.example.myapplication
  - MainActivity.kt: Główny punkt wejściowy aplikacji, zarządza nawigacją (NavHost) oraz inicjalizacją ViewModelu. Poprawiono obsługę wychodzenia z lobby (resetowanie stanu).

- com.example.myapplication.data
  - AppDatabase.kt: Konfiguracja bazy danych Room. Zawiera definicję bazy oraz interfejs DAO (GameDao) do operacji na danych.

- com.example.myapplication.model
  - GameModels.kt: Plik zawierający wszystkie modele danych (Data Classes) używane w aplikacji, takie jak Player, GameSettings, ScoreEntry oraz encja bazy danych GameSave.

- com.example.myapplication.viewmodel
  - LobbyViewModel.kt: Serce logiki aplikacji. Zarządza stanem (pieniądze, gracze), komunikacją sieciową (Socket) oraz interakcją z bazą danych. Dodano funkcję leaveLobby() oraz cleanupSockets(), które gwarantują całkowite zamknięcie połączeń i wyczyszczenie danych przy opuszczaniu pokoju.

- com.example.myapplication.ui.screens
  - MainMenu.kt: Menu główne aplikacji.
  - ClickerScreen.kt: Ekran "klikacza", pozwalający zarabiać monety.
  - GameScreen.kt: Główna minigra z wykresem, gdzie gracze obstawiają mnożniki.
  - GameSettingsScreen.kt: Konfiguracja parametrów gry przed hostowaniem.
  - HostLobbyScreen.kt: Ekran dla hosta gry, wyświetlający IP i listę graczy.
  - JoinLobbyScreen.kt: Ekran pozwalający dołączyć do istniejącej gry poprzez IP.
  - ProfileScreen.kt: Ekran edycji profilu (nazwa, awatar).
  - LeaderboardScreen.kt: Ekran wyników po zakończeniu gry.

- com.example.myapplication.ui.theme
  - Color.kt, Theme.kt, Type.kt: Definicje kolorystyki, motywu i typografii (Material 3).

2. JAK DZIAŁA PROJEKT?

Aplikacja "VAULT" to symulator gry hazardowo-inwestycyjnej z elementami społecznościowymi.
- Zarabianie: Użytkownik może zarabiać "monety" w prostym klikaczu.
- Profil: Każdy gracz może ustawić swoją nazwę i wybrać jeden z dostępnych awatarów. Dane są zapisywane w lokalnej bazie Room.
- Multiplayer: Gra wspiera tryb wieloosobowy w sieci lokalnej. Jeden gracz staje się hostem (tworzy serwer na porcie 8888), a inni dołączają wpisując jego adres IP.
- Rozgrywka: Host ustala parametry (ilość rund, blind, zmienność), a następnie wszyscy gracze biorą udział w grze z wykresem. Celem jest zatrzymanie się na jak najwyższym mnożniku, zanim wykres "uderzy" w przeszkodę.
- Wyniki: Po zakończeniu rundy wyświetlana jest tabela liderów, a zwycięzca otrzymuje nagrodę.

PROCES POPRAWEK:
- Naprawiono błąd "pamiętania" poprzedniego lobby poprzez dodanie jawnego czyszczenia gniazd (sockets) i list graczy przy każdym powrocie do menu lub rozpoczęciu nowej sesji.
