package ViewModel;
import Model.Model;

public class ViewModel {

    private Model model;
    public ViewModel(Model model) {
        this.model = model;
    }

    public void getStem(boolean isStem) {
        model.setStem(isStem);
    }
}
