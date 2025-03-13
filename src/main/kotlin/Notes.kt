//Application {
//    Start_AddNote_Activity {
//        Change_Text {
//            ClickTo_Char {}
//            ClickTo_Remove {}
//            ClickTo_Action {}// Next, Done
//        }
//        ClickTo_Undo_Change {}
//        ClickTo_Redo_Change {}
//        ClickTo_Cancel_Activity {}
//        ClickTo_Finish_Activity {}
//    }
//    Start_ReadNotes_Activity {
//        ClickTo_Filter_Query {
//            Ui.Input_Text(App.Change_Query_Text(value), Platform.Save_Query_Text(value))
//            Ui.ClickTo_Clear_Button(App.Change_Query_Text(""), Platform.Save_Query_Text(""))
//        }
//        Scroll_List_Notes {}
//        ClickTo_NoteItem(
//            Start_Note_Activity {
//                ClickTo_Remove_Button(Remove_Note)
//                Change_Text(Save_Note_Text)
//                ClickTo_Undo(Remove_Note_Text_Change)
//                ClickTo_Redo(Restore_Note_Text_Change)
//                ClickTo_Back_Button(Cancel_Activity{})
//                ClickTo_Done_Button(Finish_Activity{})
//                ClickTo_Like_Button(Change_Note_Like)
//            }
//        )
//        ClickTo_Remove_Button(Remove_Note)
//        ClickTo_Like_Button(Change_Note_Like)
//    }
//}
//{
//    StartScreen UseCase() -> ScreenView with DisposedEffect ->UseCaseStart, UseCaseClose
//    ClickTo UseCase() -> Button(onClick -> UseCase) // список вызова каждого StartScreen
//    InputText UseCase() -> EditText(onChange -> UseCase)
//    ScrollTo ItemsUseCase()-> LazyColumn(onScroll -> UseCase, onItemClick -> UseCase)
//    LookAtText -> Text
//    LookAtImage -> Icon || Coil
//
//    // сразу запускать
//    ScreenView {
//        val elements = listOf<Element>(
//            ButtonElement(),
//            TextFieldElement(),
//            ListElement(), Row, Column, LazyColumn, LazyRow |
//        BoxElement()
//        TextElement(),
//        ImageElement(), Icon, Coil
//        )
//        elements.forEach {
//            when (it) {
//                is ButtonElement -> Button(it.text)
//                is ImageElement -> Icon(it.text) || Coil(it.url)
//                is ListElement -> LazyColumn()
//            }
//        }
//    }
//}